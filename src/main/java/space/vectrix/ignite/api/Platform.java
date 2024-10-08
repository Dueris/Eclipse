package space.vectrix.ignite.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import space.vectrix.ignite.api.mod.Mods;

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
