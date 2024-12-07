package io.github.dueris.eclipse.loader;

import io.github.dueris.eclipse.loader.api.Platform;
import io.github.dueris.eclipse.loader.api.mod.Mods;
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
		return EclipseLoaderBootstrap.instance().engine();
	}
}
