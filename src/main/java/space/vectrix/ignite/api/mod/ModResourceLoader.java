package space.vectrix.ignite.api.mod;

import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.tinylog.Logger;
import space.vectrix.ignite.api.util.IgniteConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Represents the mod resource loader.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class ModResourceLoader {
	/* package */
	@NotNull List<ModContainerImpl> loadResources(final @NotNull ModsImpl engine) {
		final List<ModContainerImpl> containers = new ArrayList<>();

		for (final ModResource resource : engine.resources()) {
			if (resource.locator().equals(ModResourceLocator.LAUNCHER_LOCATOR) || resource.locator().equals(ModResourceLocator.GAME_LOCATOR)) {
				continue;
			}

			final Path resourcePath = resource.path();
			try (final JarFile jarFile = new JarFile(resourcePath.toFile())) {
				JarEntry jarEntry = jarFile.getJarEntry(IgniteConstants.MOD_CONFIG_YML);
				if (jarEntry == null) {
					jarEntry = jarFile.getJarEntry(IgniteConstants.MOD_CONFIG_YAML);

					if (jarEntry == null) {
						continue;
					}
				}

				final InputStream inputStream = jarFile.getInputStream(jarEntry);
				final ModConfig config = ModConfig.init(YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream)));
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
}
