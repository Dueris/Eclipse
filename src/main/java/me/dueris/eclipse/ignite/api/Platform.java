package me.dueris.eclipse.ignite.api;

import me.dueris.eclipse.ignite.api.mod.Mods;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to the main functions of Ignite.
 *
 * @author vectrix
 * @since 1.0.0
 */
@ApiStatus.NonExtendable
public interface Platform {
	/**
	 * Returns the {@link Mods}.
	 *
	 * @return the mods
	 * @since 1.0.0
	 */
	@NotNull Mods mods();
}
