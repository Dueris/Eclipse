package io.github.dueris.eclipse.api.mod;

import io.github.dueris.eclipse.api.game.GameProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApiStatus.NonExtendable
public interface ModEngine {
	boolean loaded(final @NotNull String id);

	@NotNull Optional<ModContainer> container(final @NotNull String id);

	@NotNull List<ModResource> resources();

	@NotNull Collection<ModContainer> containers();

	@NotNull GameProvider gameProvider();

	/**
	 * Only returns null if the ModResource isn't registered to a ModContainer, or it's the Launcher or Game resource.
	 */
	@Nullable ModContainer getContainerFromResource(ModResource modResource);

	@NotNull ModResource getLauncherResource();

	@NotNull ModResource getGameResource();
}
