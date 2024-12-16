package io.github.dueris.eclipse.plugin.access;

import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModMetadata;
import org.jetbrains.annotations.Nullable;

/**
 * API for getting Ignite/Eclipse-related mod metadata
 * If the {@link ModContainer} or {@link ModMetadata} is null, its not an eclipse plugin
 */
public interface MixinPlugin {
	/**
	 * Gets the {@link ModContainer} for this plugin instance
	 */
	@Nullable ModContainer eclipse$getModContainer();

	/**
	 * Gets the {@link ModMetadata} for this plugin instance
	 */
	@Nullable ModMetadata eclipse$getModConfig();
}
