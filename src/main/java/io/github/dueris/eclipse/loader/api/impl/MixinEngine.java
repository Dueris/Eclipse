package io.github.dueris.eclipse.loader.api.impl;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.agent.IgniteAgent;
import io.github.dueris.eclipse.loader.api.Blackboard;
import io.github.dueris.eclipse.loader.api.mod.*;
import io.github.dueris.eclipse.loader.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.launch.ember.EmberMixinContainer;
import io.github.dueris.eclipse.loader.launch.ember.EmberMixinService;
import io.github.dueris.eclipse.loader.launch.ember.EmberTransformer;
import io.github.dueris.eclipse.loader.launch.transformer.AccessTransformerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MixinEngine implements Engine {
	public static final String JAVA_LOCATOR = "java_locator";
	public static final String LAUNCHER_LOCATOR = "launcher_locator";
	public static final String GAME_LOCATOR = "game_locator";
	private static final Map<Path, List<ModResource>> parentToChildren = new HashMap<>();
	private final Map<String, ModContainer> containersByConfig = new LinkedHashMap<>();
	private final Map<String, ModContainer> containers = new LinkedHashMap<>();
	private final List<ModResource> resources = new LinkedList<>();

	@Override
	public boolean loaded(final @NotNull String id) {
		return this.containers.containsKey(id);
	}

	@Override
	public @NotNull Optional<ModContainer> container(final @NotNull String id) {
		return Optional.ofNullable(this.containers.get(id));
	}

	@Override
	public @NotNull @UnmodifiableView List<ModResource> resources() {
		return Collections.unmodifiableList(this.resources);
	}

	@Override
	public @NotNull @UnmodifiableView Collection<ModContainer> containers() {
		return Collections.unmodifiableCollection(this.containers.values());
	}

	public @Nullable ModContainerImpl getContainerFromResource(@NotNull ModResource resource) {
		if (resource.locator().equalsIgnoreCase(GAME_LOCATOR) || resource.locator().equalsIgnoreCase(LAUNCHER_LOCATOR))
			return null;
		return (ModContainerImpl) containers.values().stream().filter(c -> c.resource().equals(resource)).findFirst().orElse(null);
	}

	public boolean locateResources() {
		return this.resources.addAll(this.prepareResources());
	}

	public void resolveResources() {
		for (final ModContainerImpl container : loadResources()) {
			final ModResource resource = container.resource();

			if (!resource.locator().equals(LAUNCHER_LOCATOR) && !resource.locator().equals(GAME_LOCATOR)) {
				try {
					IgniteAgent.addJar(container.resource().path());
				} catch (final IOException exception) {
					Logger.error(exception, "Unable to add container '{}' to the classpath!", container.id());
				}
			}

			this.containers.put(container.id(), container);
		}

	}

	private @NotNull List<ModContainerImpl> loadResources() {
		final List<ModContainerImpl> containers = new ArrayList<>();

		for (final ModResource resource : this.resources()) {
			if (resource.locator().equals(LAUNCHER_LOCATOR) || resource.locator().equals(GAME_LOCATOR)) {
				continue;
			}

			final Path resourcePath = resource.path();
			try (final JarFile jarFile = new JarFile(resourcePath.toFile())) {
				JarEntry jarEntry = jarFile.getJarEntry(IgniteConstants.MOD_CONFIG_YML);
				if (jarEntry == null) {
					continue;
				}

				final InputStream inputStream = jarFile.getInputStream(jarEntry);
				final ModMetadata config = ModMetadata.read(YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream)));
				if (config.mixins().isEmpty() && config.wideners().isEmpty()) {
					continue;
				}

				containers.add(new ModContainerImpl(Logger.tag(config.id()), resource, config));
			} catch (final IOException exception) {
				// Ignore
			}
		}

		return containers;
	}

	public void resolveWideners(final @NotNull EmberTransformer transformer) {
		final AccessTransformerImpl accessTransformer = transformer.transformer(AccessTransformerImpl.class);
		if (accessTransformer == null) return;

		for (final ModContainer container : this.containers()) {
			final ModResource resource = container.resource();

			final List<String> wideners = container.config().wideners();
			if (!wideners.isEmpty()) {
				for (final String widener : wideners) {
					//noinspection resource
					final Path path = resource.fileSystem().getPath(widener);
					try {
						Logger.trace("Adding the access widener: {}", widener);
						accessTransformer.addWidener(path);
					} catch (final IOException exception) {
						Logger.trace(exception, "Failed to configure widener: {}", widener);
						continue;
					}

					Logger.trace("Added the access widener: {}", widener);
				}
			}
		}
	}

	public void resolveMixins() {
		final EmberMixinService service = (EmberMixinService) MixinService.getService();
		final EmberMixinContainer handle = (EmberMixinContainer) service.getPrimaryContainer();

		for (final ModContainer container : this.containers()) {
			final ModResource resource = container.resource();

			handle.addResource(resource.path().getFileName().toString(), resource.path());

			final List<String> mixins = container.config().mixins();
			if (!mixins.isEmpty()) {
				for (final String config : mixins) {
					final ModContainer previous = this.containersByConfig.putIfAbsent(config, container);
					if (previous != null) {
						Logger.warn("Skipping duplicate mixin configuration: {} (in {} and {})", config, previous.id(), container.id());
						continue;
					}

					Mixins.addConfiguration(config);
				}

				Logger.trace("Added the mixin configurations: {}", String.join(", ", mixins));
			}
		}

		for (final Config config : Mixins.getConfigs()) {
			final ModContainer container = this.containersByConfig.get(config.getName());
			if (container == null) continue;

			final IMixinConfig mixinConfig = config.getConfig();
			mixinConfig.decorate(FabricUtil.KEY_MOD_ID, container.id());
			mixinConfig.decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST);
		}
	}

	@SuppressWarnings({"ResultOfMethodCallIgnored", "resource"})
	public @NotNull List<ModResourceImpl> prepareResources() {
		final List<ModResourceImpl> resources = new ArrayList<>();

		// Add the launcher and game resources.
		resources.add(this.createLauncherResource());
		resources.add(this.createGameResource());

		// Retrieve the mods from the mods directory.
		final Path modDirectory = Blackboard.raw(Blackboard.MODS_DIRECTORY);
		final Path cachedModsDirectory = Paths.get(".").toAbsolutePath().resolve("cache").resolve(".eclipse").resolve("processedMods");
		try {
			if (modDirectory == null) {
				throw new RuntimeException("Failed to get mods directory!");
			}

			if (Files.notExists(modDirectory)) {
				modDirectory.toFile().mkdirs();
			}

			if (Files.notExists(cachedModsDirectory)) {
				Files.createDirectories(cachedModsDirectory);
			}

			if (!Files.isDirectory(cachedModsDirectory)) {
				throw new RuntimeException("cachedModsDirectory was not created successfully: " + cachedModsDirectory);
			}

			Files.walk(cachedModsDirectory)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						throw new RuntimeException("Failed to delete: " + p, e);
					}
				});

			List<Path> toInspect = new ArrayList<>(Files.list(modDirectory)
				.filter(Files::isRegularFile)
				.toList()
			);
			if (EclipseLoaderBootstrap.INSTANCE.context.isProviderContext()) {
				toInspect.add(EclipseLoaderBootstrap.ROOT_ABSOLUTE);
			}

			AtomicBoolean childLoading = new AtomicBoolean(true);
			Function<Path, ModResource> resourceBuilder = (childDirectory) -> {
				ModResourceImpl resource;
				if (!Files.isRegularFile(childDirectory) || !childDirectory.getFileName().toString().endsWith(".jar")) {
					return null;
				}

				try (final JarFile jarFile = new JarFile(childDirectory.toFile())) {
					JarEntry jarEntry = jarFile.getJarEntry(IgniteConstants.MOD_CONFIG_YML);
					if (jarEntry == null) {
						return null;
					}

					resource = new ModResourceImpl(JAVA_LOCATOR, childDirectory, jarFile.getManifest(), childLoading.get(), parentToChildren.getOrDefault(childDirectory, List.of()));
					resources.add(resource);
					return resource;
				} catch (IOException e) {
					throw new RuntimeException("Failed to walk child file when reading mod resource!", e);
				}
			};

			prepareCached(modDirectory, cachedModsDirectory, resourceBuilder);
			childLoading.set(false);
			toInspect.forEach(resourceBuilder::apply);
		} catch (final Throwable throwable) {
			throw new RuntimeException("Failed to walk the mods directory!", throwable);
		}

		return resources;
	}

	private void prepareCached(Path modDirectory, Path cacheDir, Function<Path, ModResource> resourceBuilder) throws Throwable {
		//noinspection resource
		List<Path> toInspect = new ArrayList<>(Files.list(modDirectory)
			.filter(Files::isRegularFile)
			.toList()
		);
		for (final Path childDirectory : toInspect) {
			if (!Files.isRegularFile(childDirectory) || !childDirectory.getFileName().toString().endsWith(".jar")) {
				continue;
			}

			try (final JarFile jarFile = new JarFile(childDirectory.toFile())) {
				JarEntry modsDir = jarFile.getJarEntry("META-INF/mods/");
				if (modsDir != null && modsDir.isDirectory()) {
					jarFile.stream().filter(e -> e.getName().startsWith("META-INF/mods/") && !e.isDirectory()).forEach(entry -> {
						if (!parentToChildren.containsKey(childDirectory)) {
							parentToChildren.put(childDirectory, new ArrayList<>());
						}
						String fileName = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
						Path outputFile = cacheDir.resolve(fileName);

						try {
							Logger.trace("Building processed mod : " + entry.getName());
							Files.createDirectories(cacheDir);

							if (!entry.isDirectory()) {
								try (InputStream inputStream = jarFile.getInputStream(entry)) {
									Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
								}
							}

							if (!entry.isDirectory()) {
								Logger.trace("Validating...");
								validateExtractedFile(outputFile);
								ModResourceImpl i = (ModResourceImpl) resourceBuilder.apply(outputFile);
								if (i != null) {
									parentToChildren.get(childDirectory).add(i);
								}
							}

						} catch (IOException e) {
							throw new RuntimeException("Failed to process jar entry: " + entry.getName(), e);
						}
					});
				}
			}
		}
	}

	private void validateExtractedFile(@NotNull Path file) throws IOException {
		try (JarFile jarFile = new JarFile(file.toFile())) {
			jarFile.stream().forEach(entry -> {
				try (InputStream is = jarFile.getInputStream(entry)) {
					//noinspection StatementWithEmptyBody
					while (is.read() != -1) ;
				} catch (IOException e) {
					throw new RuntimeException("Invalid jar entry detected: " + entry.getName(), e);
				}
			});
		}
	}

	private @NotNull ModResourceImpl createLauncherResource() {
		final File launcherFile;
		try {
			launcherFile = new File(EclipseLoaderBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (final URISyntaxException exception) {
			throw new RuntimeException("Failed to get launcher path!", exception);
		}

		try (final JarFile jarFile = new JarFile(launcherFile)) {
			return new ModResourceImpl(LAUNCHER_LOCATOR, launcherFile.toPath(), jarFile.getManifest(), false, List.of());
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get launcher manifest!", exception);
		}
	}

	private @NotNull ModResourceImpl createGameResource() {
		final File gameFile = Blackboard.raw(Blackboard.GAME_JAR).toFile();
		try (final JarFile jarFile = new JarFile(gameFile)) {
			return new ModResourceImpl(GAME_LOCATOR, gameFile.toPath(), jarFile.getManifest(), false, List.of());
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get game manifest!", exception);
		}
	}
}