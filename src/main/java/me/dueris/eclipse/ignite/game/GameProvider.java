package me.dueris.eclipse.ignite.game;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Represents a game resource provider.
 *
 * @author vectrix
 * @since 1.0.0
 */
public interface GameProvider {
	/**
	 * Returns a stream of library paths to load.
	 *
	 * @return the game library paths
	 * @since 1.0.0
	 */
	@NotNull Stream<Path> gameLibraries();

	/**
	 * Returns the game path.
	 *
	 * @return the game path
	 * @since 1.0.0
	 */
	@NotNull Path gamePath();
}
