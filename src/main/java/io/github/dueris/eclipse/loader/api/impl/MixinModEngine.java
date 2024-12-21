package io.github.dueris.eclipse.loader.api.impl;

import io.github.dueris.eclipse.api.game.GameProvider;
import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModEngine;
import io.github.dueris.eclipse.api.mod.ModMetadata;
import io.github.dueris.eclipse.api.mod.ModResource;
import io.github.dueris.eclipse.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.EclipseLauncher;
import io.github.dueris.eclipse.loader.Main;
import io.github.dueris.eclipse.loader.MixinJavaAgent;
import io.github.dueris.eclipse.loader.ember.EmberMixinService;
import io.github.dueris.eclipse.loader.ember.mixin.EmberMixinContainer;
import io.github.dueris.eclipse.loader.ember.patch.EmberTransformer;
import io.github.dueris.eclipse.loader.launch.transformer.AccessWidenerTransformer;
import io.github.dueris.eclipse.loader.minecraft.MinecraftGameProvider;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MixinModEngine implements ModEngine {
	public static final String JAVA_LOCATOR = "java_locator";
	public static final String LAUNCHER_LOCATOR = "launcher_locator";
	public static final String GAME_LOCATOR = "game_locator";
	private static final AtomicReference<ModResource> CACHED_GAME_RESOURCE = new AtomicReference<>(null);
	private static final AtomicReference<ModResource> CACHED_LAUNCHER_RESOURCE = new AtomicReference<>(null);
	private static final Map<Path, List<ModResource>> parentToChildren = new HashMap<>();
	private final Map<String, ModContainer> containersByConfig = new LinkedHashMap<>();
	private final Map<String, ModContainer> containers = new LinkedHashMap<>();
	private final List<ModResource> resources = new LinkedList<>();
	private final GameProvider provider;

	public MixinModEngine() {
		this.provider = new MinecraftGameProvider();
	}

	private static @Nullable ModResourceImpl validateAndCreateResource(JarEntry entry, Path childFile, @NotNull JarFile jarFile, @NotNull AtomicBoolean childLoading, @NotNull List<ModResourceImpl> resources) throws IOException {
		YamlConfiguration preloadedConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(jarFile.getInputStream(entry)));
		if (preloadedConfig.contains("mixins") || preloadedConfig.contains("wideners") || preloadedConfig.contains("datapack-entry")) {
			ModResourceImpl resource = new ModResourceImpl(JAVA_LOCATOR, childFile, jarFile.getManifest(), childLoading.get(), parentToChildren.getOrDefault(childFile, List.of()));
			resources.add(resource);
			return resource;
		}
		return null; // Invalid
	}

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

	@Override
	public @NotNull GameProvider gameProvider() {
		return provider;
	}

	@Override
	public @Nullable ModContainerImpl getContainerFromResource(@NotNull ModResource resource) {
		if (resource.locator().equalsIgnoreCase(GAME_LOCATOR) || resource.locator().equalsIgnoreCase(LAUNCHER_LOCATOR))
			return null;
		return (ModContainerImpl) containers.values().stream().filter(c -> c.resource().equals(resource)).findFirst()
											.orElse(null);
	}

	public boolean locateResources() {
		return this.resources.addAll(this.prepareResources());
	}

	public void resolveResources() {
		for (final ModContainerImpl container : loadResources()) {
			final ModResource resource = container.resource();

			if (!resource.locator().equals(LAUNCHER_LOCATOR) && !resource.locator().equals(GAME_LOCATOR)) {
				try {
					MixinJavaAgent.appendToClassPath(container.resource().path());
				} catch (final Throwable exception) {
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
		final AccessWidenerTransformer accessTransformer = transformer.getTransformer(AccessWidenerTransformer.class);
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
		final Path modDirectory = (Path) EclipseLauncher.INSTANCE.getProperties().get("modspath");
		final Path cachedModsDirectory = Paths.get(".").toAbsolutePath().resolve("cache").resolve(".eclipse")
											  .resolve("processedMods");
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
			if (EclipseLauncher.INSTANCE.entryContext().isProviderContext()) {
				toInspect.add(Main.ROOT_ABSOLUTE);
			}

			AtomicBoolean childLoading = new AtomicBoolean(true);
			Function<Path, ModResource> resourceBuilder = (childFile) -> {
				if (!Files.isRegularFile(childFile) || !childFile.getFileName().toString().endsWith(".jar")) {
					return null;
				}

				try (final JarFile jarFile = new JarFile(childFile.toFile())) {
					Manifest manifest = jarFile.getManifest();
					if (manifest != null && (manifest.getMainAttributes()
													 .getValue("paperweight-mappings-namespace") != null && !manifest.getMainAttributes()
																													 .getValue("paperweight-mappings-namespace")
																													 .equals("mojang+yarn"))) {
						Logger.warn("JarFile, {}, doesn't have a safe mappings namespace(recommended: {}, but found {})! If this is a mixin plugin, it might not work. Proceed with caution", jarFile.getName(), "mojang+yarn", manifest.getMainAttributes()
																																																										.getValue("paperweight-mappings-namespace"));
					}
					JarEntry jarEntry = jarFile.getJarEntry(IgniteConstants.MOD_CONFIG_YML);
					if (jarEntry == null) {
						return null;
					}

					return validateAndCreateResource(jarEntry, childFile, jarFile, childLoading, resources);
				} catch (Throwable e) {
					e.printStackTrace();
					return null;
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
					jarFile.stream().filter(e -> e.getName().startsWith("META-INF/mods/") && !e.isDirectory())
						   .forEach(entry -> {
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
		if (CACHED_LAUNCHER_RESOURCE.get() != null) {
			// Launcher resource was cached, use that.
			return (@NotNull ModResourceImpl) CACHED_LAUNCHER_RESOURCE.get();
		}
		final File launcherFile;
		try {
			launcherFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (final URISyntaxException exception) {
			throw new RuntimeException("Failed to get launcher path!", exception);
		}

		try (final JarFile jarFile = new JarFile(launcherFile)) {
			ModResourceImpl resource = new ModResourceImpl(LAUNCHER_LOCATOR, launcherFile.toPath(), jarFile.getManifest(), false, List.of());
			CACHED_LAUNCHER_RESOURCE.set(resource);
			return resource;
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get launcher manifest!", exception);
		}
	}

	private @NotNull ModResourceImpl createGameResource() {
		if (CACHED_GAME_RESOURCE.get() != null) {
			// Game resource was cached, use that.
			return (@NotNull ModResourceImpl) CACHED_GAME_RESOURCE.get();
		}
		final File gameFile = ((Path) EclipseLauncher.INSTANCE.getProperties().get("gamejar")).toFile();
		try (final JarFile jarFile = new JarFile(gameFile)) {
			ModResourceImpl resource = new ModResourceImpl(GAME_LOCATOR, gameFile.toPath(), jarFile.getManifest(), false, List.of());
			CACHED_GAME_RESOURCE.set(resource);
			return resource;
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get game manifest!", exception);
		}
	}

	@Override
	public @NotNull ModResource getLauncherResource() {
		return createLauncherResource();
	}

	@Override
	public @NotNull ModResource getGameResource() {
		return createGameResource();
	}
}
