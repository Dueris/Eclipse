package io.github.dueris.eclipse.loader.api.mod;

import io.github.dueris.eclipse.loader.api.impl.ModContainerImpl;
import io.github.dueris.eclipse.loader.api.impl.ModsImpl;
import io.github.dueris.eclipse.loader.api.util.IgniteConstants;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.tinylog.Logger;

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

	public @NotNull List<ModContainerImpl> loadResources(final @NotNull ModsImpl engine) {
		final List<ModContainerImpl> containers = new ArrayList<>();

		for (final ModResource resource : engine.resources()) {
			if (resource.locator().equals(ModResourceLocator.LAUNCHER_LOCATOR) || resource.locator().equals(ModResourceLocator.GAME_LOCATOR)) {
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
}
