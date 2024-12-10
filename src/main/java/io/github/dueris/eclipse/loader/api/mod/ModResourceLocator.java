package io.github.dueris.eclipse.loader.api.mod;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.api.Blackboard;
import io.github.dueris.eclipse.loader.api.impl.ModResourceImpl;
import io.github.dueris.eclipse.loader.api.util.IgniteConstants;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * Represents the mod resource locator.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class ModResourceLocator {
	public static final String JAVA_LOCATOR = "java_locator";
	public static final String LAUNCHER_LOCATOR = "launcher_locator";
	public static final String GAME_LOCATOR = "game_locator";
	private static final Map<Path, List<ModResource>> parentToChildren = new HashMap<>();


	@SuppressWarnings({"ResultOfMethodCallIgnored", "resource"})
	public @NotNull List<ModResourceImpl> locateResources() {
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

					resource = new ModResourceImpl(ModResourceLocator.JAVA_LOCATOR, childDirectory, jarFile.getManifest(), childLoading.get(), parentToChildren.getOrDefault(childDirectory, List.of()));
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
			return new ModResourceImpl(ModResourceLocator.LAUNCHER_LOCATOR, launcherFile.toPath(), jarFile.getManifest(), false, List.of());
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get launcher manifest!", exception);
		}
	}

	private @NotNull ModResourceImpl createGameResource() {
		final File gameFile = Blackboard.raw(Blackboard.GAME_JAR).toFile();
		try (final JarFile jarFile = new JarFile(gameFile)) {
			return new ModResourceImpl(ModResourceLocator.GAME_LOCATOR, gameFile.toPath(), jarFile.getManifest(), false, List.of());
		} catch (final Exception exception) {
			throw new RuntimeException("Failed to get game manifest!", exception);
		}
	}
}
