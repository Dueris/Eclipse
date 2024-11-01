package me.dueris.eclipse.access;

import me.dueris.eclipse.ignite.api.mod.ModConfig;
import me.dueris.eclipse.ignite.api.mod.ModContainer;
import org.jetbrains.annotations.Nullable;

/**
 * API for getting Ignite/Eclipse-related mod metadata
 * If the {@link ModContainer} or {@link ModConfig} is null, its not an eclipse plugin
 */
public interface MixinPlugin {
	/**
	 * Gets the {@link ModContainer} for this plugin instance
	 */
	@Nullable ModContainer eclipse$getModContainer();

	/**
	 * Gets the {@link ModConfig} for this plugin instance
	 */
	@Nullable ModConfig eclipse$getModConfig();
}
