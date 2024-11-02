package me.dueris.eclipse.ignite;

import me.dueris.eclipse.ignite.api.Platform;
import me.dueris.eclipse.ignite.api.mod.Mods;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the platform implementation.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class PlatformImpl implements Platform {
	PlatformImpl() {
	}

	@Override
	public @NotNull Mods mods() {
		return IgniteBootstrap.instance().engine();
	}
}
