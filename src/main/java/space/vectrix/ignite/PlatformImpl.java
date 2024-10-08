package space.vectrix.ignite;

import org.jetbrains.annotations.NotNull;
import space.vectrix.ignite.api.Platform;
import space.vectrix.ignite.api.mod.Mods;

/**
 * Provides the platform implementation.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class PlatformImpl implements Platform {
	/* package */ PlatformImpl() {
	}

	@Override
	public @NotNull Mods mods() {
		return IgniteBootstrap.instance().engine();
	}
}
