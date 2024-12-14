package io.github.dueris.eclipse.loader.launch;

import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.api.impl.MixinEngine;
import io.github.dueris.eclipse.loader.api.impl.ModContainerImpl;
import io.github.dueris.eclipse.loader.api.impl.ModMetadata;
import io.github.dueris.eclipse.loader.api.impl.ModResourceImpl;
import io.github.dueris.eclipse.loader.api.mod.ModResource;
import io.github.dueris.eclipse.loader.api.util.ClassLoaders;
import io.github.dueris.eclipse.loader.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.api.util.IgniteExclusions;
import io.github.dueris.eclipse.loader.launch.ember.EmberClassLoader;
import io.github.dueris.eclipse.loader.launch.ember.transformer.EmberTransformer;
import io.github.dueris.eclipse.loader.util.LaunchException;
import io.github.dueris.eclipse.plugin.access.EclipseMain;
import joptsimple.OptionSet;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Provides the launch handling for Ignite to Ember.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class EmberLauncher {
	private static final Map<String, Object> properties = new LinkedHashMap<>();
	private static final String JAVA_HOME = System.getProperty("java.home");
	private static final @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Manifest> DEFAULT_MANIFEST = Optional.of(new Manifest());

	private final ConcurrentMap<String, Optional<Manifest>> manifests = new ConcurrentHashMap<>();

	public static Map<String, Object> getProperties() {
		return properties;
	}

	public void initialize() {
		// Initialize the mod engine.
		final MixinEngine engine = EclipseLoaderBootstrap.instance().engine();
		if (engine.locateResources()) {
			engine.resolveResources();

			StringBuilder builder = new StringBuilder();
			EclipseLoaderBootstrap bootstrap = EclipseLoaderBootstrap.instance();
			builder.append("Found {} mod(s):\n".replace("{}", String.valueOf(bootstrap.engine().containers().size() + 3)));
			for (ModResource container : bootstrap.engine().resources()) {
				ModResourceImpl realResource = (ModResourceImpl) container;
				ModContainerImpl modContainer = bootstrap.engine().getContainerFromResource(realResource);
				if (modContainer == null) {
					String locator = realResource.locator();
					if (locator.equalsIgnoreCase(MixinEngine.LAUNCHER_LOCATOR)) {
						builder.append("\t- ").append("eclipseloader ").append(IgniteConstants.IMPLEMENTATION_VERSION).append("\n");
						builder.append("\t   \\-- mixinextras ").append(MixinExtrasVersion.LATEST.toString()).append("\n");
					} else if (locator.equalsIgnoreCase(MixinEngine.GAME_LOCATOR)) {
						builder.append("\t- ").append("minecraft ").append(bootstrap.versionString).append("\n");
					} else {
						throw new NullPointerException("Unable to find container impl for resource of mod! : " + realResource);
					}
				} else {
					ModMetadata metadata = modContainer.config();
					if (realResource.isChild()) {
						continue;
					}
					builder.append("\t- ").append(metadata.id().replace("@", " ")).append("\n");

					List<ModResource> children = realResource.getChildren();
					int i = 0;
					while (i < children.size()) {
						ModContainerImpl childContainer = bootstrap.engine().getContainerFromResource(children.get(i));
						if (childContainer == null) {
							i++;
							continue;
						}

						builder.append(((i + 1) == children.size()) ? "\t   \\-- " : "\t   |-- ").append(childContainer.id()).append(" ").append(childContainer.version()).append("\n");
						i++;
					}
				}
			}
			Logger.info(builder.substring(0, builder.length() - 1));
		}
	}

	public void configure(final @NotNull EmberClassLoader classLoader, final @NotNull EmberTransformer transformer) {
		for (final URL url : ClassLoaders.systemClassPaths()) {
			try {
				final URI uri = url.toURI();
				if (!this.transformable(uri)) {
					Logger.debug("Skipped adding transformation path for: {}", url);
					continue;
				}

				classLoader.addTransformationPath(Paths.get(url.toURI()));
				Logger.debug("Added transformation path for: {}", url);
			} catch (final URISyntaxException | IOException exception) {
				Logger.error(exception, "Failed to add transformation path for: {}", url);
			}
		}

		classLoader.addTransformationFilter(this.packageFilter());
		classLoader.addManifestLocator(this.manifestLocator());
		transformer.addResourceExclusion(this.resourceFilter());
	}

	public void prepare(final @NotNull EmberTransformer transformer) {
		final MixinEngine engine = EclipseLoaderBootstrap.instance().engine();

		// Resolve the wideners.
		engine.resolveWideners(transformer);

		// Resolve the mixins.
		engine.resolveMixins();
	}

	public void launch(final @NotNull OptionSet optionSet, final @NotNull EmberClassLoader loader) throws LaunchException {
		try {
			final Path gameJar = (Path) getProperties().get("gamejar");
			final String gameTarget = EclipseGameLocator.targetClass();
			if (gameJar != null && Files.exists(gameJar)) {
				Object instance = Class.forName(gameTarget, true, loader).getConstructor().newInstance();
				EclipseMain.class.getMethod("eclipse$main", OptionSet.class).invoke(instance, optionSet);
			} else {
				throw new IllegalStateException("No game jar was found to launch!");
			}
		} catch (Throwable throwable) {
			throw new LaunchException("Unable to launch Minecraft server!", throwable);
		}
	}

	private @NotNull Predicate<String> packageFilter() {
		return name -> {
			for (final String test : IgniteExclusions.TRANSFORMATION_EXCLUDED_PACKAGES) {
				if (name.startsWith(test)) {
					return false;
				}
			}

			return true;
		};
	}

	private @NotNull Predicate<String> resourceFilter() {
		return path -> {
			for (final String test : IgniteExclusions.TRANSFORMATION_EXCLUDED_RESOURCES) {
				if (path.startsWith(test)) {
					return false;
				}
			}

			return true;
		};
	}

	private @NotNull Function<URLConnection, Optional<Manifest>> manifestLocator() {
		final MixinEngine engine = EclipseLoaderBootstrap.instance().engine();

		return connection -> {
			if (connection instanceof JarURLConnection) {
				final URL url = ((JarURLConnection) connection).getJarFileURL();
				final Optional<Manifest> manifest = this.manifests.computeIfAbsent(url.toString(), key -> {
					for (final ModResource resource : engine.resources()) {
						if (!resource.locator().equals(MixinEngine.JAVA_LOCATOR)) {
							continue;
						}

						try {
							if (resource.path().toAbsolutePath().normalize().equals(Paths.get(url.toURI()).toAbsolutePath().normalize())) {
								return Optional.ofNullable(resource.manifest());
							}
						} catch (final URISyntaxException exception) {
							Logger.error(exception, "Failed to load manifest from jar: {}", url);
						}
					}

					return EmberLauncher.DEFAULT_MANIFEST;
				});

				try {
					if (manifest == EmberLauncher.DEFAULT_MANIFEST) {
						return Optional.ofNullable(((JarURLConnection) connection).getManifest());
					} else {
						return manifest;
					}
				} catch (final IOException exception) {
					Logger.error(exception, "Failed to load manifest from connection for: {}", url);
				}
			}

			return Optional.empty();
		};
	}

	private boolean transformable(final @NotNull URI uri) throws URISyntaxException, IOException {
		final File target = new File(uri);

		// Ensure JVM internals are not transformable.
		if (target.getAbsolutePath().startsWith(EmberLauncher.JAVA_HOME)) {
			return false;
		}

		if (target.isDirectory()) {
			for (final String test : IgniteExclusions.TRANSFORMATION_EXCLUDED_RESOURCES) {
				if (new File(target, test).exists()) {
					return false;
				}
			}
		} else if (target.isFile()) {
			try (final JarFile jarFile = new JarFile(new File(uri))) {
				for (final String test : IgniteExclusions.TRANSFORMATION_EXCLUDED_RESOURCES) {
					if (jarFile.getEntry(test) != null) {
						return false;
					}
				}
			}
		}

		return true;
	}
}
