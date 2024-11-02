package me.dueris.eclipse.ignite.launch.ember;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.util.Objects.requireNonNull;

/**
 * Represents the transformation class loader.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class EmberClassLoader extends ClassLoader {
	private static final List<String> EXCLUDE_PACKAGES = Arrays.asList(
		"java.", "javax.", "com.sun.", "org.objectweb.asm."
	);

	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final Object lock = new Object();
	private final ClassLoader parent;
	private final DynamicClassLoader dynamic;
	private final EmberTransformer transformer;
	private final Function<URLConnection, CodeSource> sourceLocator;
	private Function<URLConnection, Manifest> manifestLocator;
	private Predicate<String> transformationFilter;

	EmberClassLoader(final @NotNull EmberTransformer transformer) {
		super(new DynamicClassLoader(new URL[0]));

		this.parent = EmberClassLoader.class.getClassLoader();
		this.dynamic = (DynamicClassLoader) this.getParent();

		this.transformer = transformer;

		this.manifestLocator = connection -> this.locateManifest(connection).orElse(null);
		this.sourceLocator = connection -> this.locateSource(connection).orElse(null);
		this.transformationFilter = name -> EmberClassLoader.EXCLUDE_PACKAGES.stream().noneMatch(name::startsWith);
	}

	/**
	 * Adds additional transformation paths.
	 *
	 * @param path a transformation path
	 * @since 1.0.0
	 */
	public void addTransformationPath(final @NotNull Path path) {
		try {
			this.dynamic.addURL(path.toUri().toURL());
		} catch (final MalformedURLException exception) {
			Logger.error(exception, "Failed to resolve transformation path: {}", path);
		}
	}

	/**
	 * Add the manifest locator.
	 *
	 * @param manifestLocator the manifest locator
	 * @since 1.0.0
	 */
	public void addManifestLocator(final @NotNull Function<URLConnection, Optional<Manifest>> manifestLocator) {
		requireNonNull(manifestLocator, "manifestLocator");
		this.manifestLocator = this.alternate(manifestLocator, this::locateManifest);
	}

	/**
	 * Add the transformation filter.
	 *
	 * @param transformationFilter a transformation filter
	 * @since 1.0.0
	 */
	public void addTransformationFilter(final @NotNull Predicate<String> transformationFilter) {
		requireNonNull(transformationFilter, "targetPackageFilter");
		this.transformationFilter = this.transformationFilter.and(transformationFilter);
	}

	//<editor-fold desc="Classes">
	boolean hasClass(final @NotNull String name) {
		final String canonicalName = name.replace('/', '.');
		return this.findLoadedClass(canonicalName) != null;
	}

	@Override
	protected @NotNull Class<?> loadClass(final @NotNull String name, final boolean resolve) throws ClassNotFoundException {
		synchronized (this.getClassLoadingLock(name)) {
			final String canonicalName = name.replace('/', '.');

			Class<?> target = this.findLoadedClass(canonicalName);
			if (target == null) {
				if (canonicalName.startsWith("java.")) {
					Logger.trace("Loading parent class: {}", canonicalName);
					target = this.parent.loadClass(canonicalName);
					Logger.trace("Loaded parent class: {}", canonicalName);
				} else {
					Logger.trace("Attempting to load class: {}", canonicalName);
					target = this.findClass(canonicalName, TransformPhase.INITIALIZE);
					if (target == null) {
						Logger.trace("Unable to locate class: {}", canonicalName);

						Logger.trace("Attempting to load parent class: {}", canonicalName);
						try {
							target = this.parent.loadClass(canonicalName);
							Logger.trace("Loaded parent class: {}", canonicalName);
						} catch (final ClassNotFoundException exception) {
							Logger.trace("Unable to locate parent class: {}", canonicalName);
							throw exception;
						}
					} else {
						Logger.trace("Loaded transformed class: {}", canonicalName);
					}
				}
			}

			if (resolve) this.resolveClass(target);
			return target;
		}
	}

	@Override
	protected @NotNull Class<?> findClass(final @NotNull String name) throws ClassNotFoundException {
		final String canonicalName = name.replace('/', '.');

		Logger.trace("Finding class: {}", canonicalName);
		final Class<?> target = this.findClass(canonicalName, TransformPhase.INITIALIZE);
		if (target == null) {
			Logger.trace("Unable to find class: {}", canonicalName);
			throw new ClassNotFoundException(canonicalName);
		}

		Logger.trace("Found class: {}", canonicalName);
		return target;
	}

	@SuppressWarnings("SameParameterValue")
	@Nullable Class<?> findClass(final @NotNull String name, final @NotNull TransformPhase phase) {
		if (name.startsWith("java.")) {
			Logger.trace("Skipping platform class: {}", name);
			return null;
		}

		// Grab the class bytes.
		final ClassData transformed = this.transformData(name, phase);
		if (transformed == null) return null;

		// Check if the class has already been loaded by the transform.
		final Class<?> existingClass = this.findLoadedClass(name);
		if (existingClass != null) {
			Logger.trace("Skipping already defined transformed class: {}", name);
			return existingClass;
		}

		// Find the package for this class.
		final int classIndex = name.lastIndexOf('.');
		if (classIndex > 0) {
			final String packageName = name.substring(0, classIndex);
			this.findPackage(packageName, transformed.manifest());
		}

		final byte[] bytes = transformed.data();
		final ProtectionDomain domain = new ProtectionDomain(transformed.source(), null, this, null);
		return this.defineClass(name, bytes, 0, bytes.length, domain);
	}

	/* package */
	@Nullable ClassData transformData(final @NotNull String name, final @NotNull TransformPhase phase) {
		final ClassData data = this.classData(name, phase);
		if (data == null) return null;

		// Prevent transforming classes that are excluded from transformation.
		if (!this.transformationFilter.test(name)) {
			Logger.trace("Skipping transformer excluded class: {}", name);
			return null;
		}

		// Run the transformation.
		final byte[] bytes = this.transformer.transform(name, data.data(), phase);
		return new ClassData(bytes, data.manifest(), data.source());
	}

	/* package */
	@Nullable ClassData classData(final @NotNull String name, final @NotNull TransformPhase phase) {
		final String resourceName = name.replace('.', '/').concat(".class");

		URL url = this.findResource(resourceName);
		if (url == null) {
			if (phase == TransformPhase.INITIALIZE) return null;
			url = this.parent.getResource(resourceName);
			if (url == null) return null;
		}

		try (final ResourceConnection connection = new ResourceConnection(url, this.manifestLocator, this.sourceLocator)) {
			final int length = connection.contentLength();
			final InputStream stream = connection.stream();
			final byte[] bytes = new byte[length];

			// @formatter:off
      int position = 0, remain = length, read;
      while((read = stream.read(bytes, position, remain)) != -1 && remain > 0) {
        position += read;
        remain -= read;
      }
      // @formatter:on

			final Manifest manifest = connection.manifest();
			final CodeSource source = connection.source();
			return new ClassData(bytes, manifest, source);
		} catch (final Exception exception) {
			Logger.trace(exception, "Failed to resolve class data: {}", resourceName);
			return null;
		}
	}

	//<editor-fold desc="Packages">
	void findPackage(final @NotNull String name, final @Nullable Manifest manifest) {
		final Package target = this.getPackage(name);
		if (target == null) {
			synchronized (this.lock) {
				if (this.getPackage(name) != null) return;

				final String path = name.replace('.', '/').concat("/");
				// @formatter:off
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        // @formatter:on

				if (manifest != null) {
					final Attributes attributes = manifest.getAttributes(path);
					if (attributes != null) {
						specTitle = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
						specVersion = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
						specVendor = attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
						implTitle = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
						implVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
						implVendor = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
					}

					final Attributes mainAttributes = manifest.getMainAttributes();
					if (mainAttributes != null) {
						if (specTitle == null) specTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
						if (specVersion == null)
							specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
						if (specVendor == null)
							specVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
						if (implTitle == null)
							implTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
						if (implVersion == null)
							implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
						if (implVendor == null)
							implVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
					}
				}

				this.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, null);
			}
		}
	}
	//</editor-fold>

	//<editor-fold desc="Resources">
	@Override
	public @Nullable URL getResource(final @NotNull String name) {
		requireNonNull(name, "name");

		URL url = this.dynamic.getResource(name);
		if (url == null) {
			url = this.parent.getResource(name);
		}

		return url;
	}
	//</editor-fold>

	@Override
	public @NotNull Enumeration<URL> getResources(final @NotNull String name) throws IOException {
		requireNonNull(name, "name");

		Enumeration<URL> resources = this.dynamic.getResources(name);
		if (!resources.hasMoreElements()) {
			resources = this.parent.getResources(name);
		}

		return resources;
	}

	@Override
	protected @Nullable URL findResource(final @NotNull String name) {
		requireNonNull(name, "name");
		return this.dynamic.findResource(name);
	}

	@Override
	protected @NotNull Enumeration<URL> findResources(final @NotNull String name) throws IOException {
		requireNonNull(name, "name");
		return this.dynamic.findResources(name);
	}

	@Override
	public @Nullable InputStream getResourceAsStream(final @NotNull String name) {
		requireNonNull(name, "name");

		InputStream stream = this.dynamic.getResourceAsStream(name);
		if (stream == null) {
			stream = this.parent.getResourceAsStream(name);
		}

		return stream;
	}

	//<editor-fold desc="Manifest">
	private @NotNull Optional<Manifest> locateManifest(final @NotNull URLConnection connection) {
		try {
			if (connection instanceof JarURLConnection) {
				return Optional.ofNullable(((JarURLConnection) connection).getManifest());
			}
		} catch (final IOException exception) {
			// Ignore
		}

		return Optional.empty();
	}
	//</editor-fold>

	private @NotNull Optional<CodeSource> locateSource(final @NotNull URLConnection connection) {
		if (connection instanceof JarURLConnection) {
			final URL url = ((JarURLConnection) connection).getJarFileURL();
			return Optional.of(new CodeSource(url, (Certificate[]) null));
		}

		return Optional.empty();
	}

	private <I, O> @NotNull Function<I, O> alternate(final @Nullable Function<I, Optional<O>> first, final @Nullable Function<I, Optional<O>> second) {
		if (second == null && first != null) return input -> first.apply(input).orElse(null);
		if (first == null && second != null) return input -> second.apply(input).orElse(null);
		if (first != null) return input -> first.apply(input).orElseGet(() -> second.apply(input).orElse(null));
		return input -> null;
	}

	@Override
	public @NotNull String getName() {
		return "ember";
	}

	private static final class DynamicClassLoader extends URLClassLoader {
		static {
			ClassLoader.registerAsParallelCapable();
		}

		DynamicClassLoader(final URL @NotNull [] urls) {
			super(urls, new DummyClassLoader());
		}

		@Override
		public void addURL(final @NotNull URL url) {
			super.addURL(url);
		}
	}

	//</editor-fold>

	/**
	 * Represents the data for a class.
	 *
	 * @author vectrix
	 * @since 1.1.0
	 */
	public static final class ClassData {
		private final byte[] data;
		private final Manifest manifest;
		private final CodeSource source;

		private ClassData(final byte[] data, final @Nullable Manifest manifest, final @Nullable CodeSource source) {
			this.data = data;
			this.manifest = manifest;
			this.source = source;
		}

		/**
		 * The class data as a byte array.
		 *
		 * @return the class data
		 * @since 1.1.0
		 */
		public byte[] data() {
			return this.data;
		}

		/**
		 * The jar manifest.
		 *
		 * @return the jar manifest
		 * @since 1.1.0
		 */
		public @Nullable Manifest manifest() {
			return this.manifest;
		}

		/**
		 * The jar code source.
		 *
		 * @return the jar code source
		 * @since 1.1.0
		 */
		public @Nullable CodeSource source() {
			return this.source;
		}
	}
}
