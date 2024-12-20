package io.github.dueris.eclipse.api.game;

import io.github.dueris.eclipse.api.GameLibrary;
import io.github.dueris.eclipse.api.McVersion;
import io.github.dueris.eclipse.api.Transformer;
import joptsimple.OptionSet;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface GameProvider {

	/**
	 * Retrieves the unique identifier for the game. EX: ("minecraft")
	 *
	 * @return The unique ID of the game.
	 */
	String getGameId();

	/**
	 * Retrieves the name of the game. EX: ("Paper")
	 *
	 * @return The name of the game.
	 */
	String getGameName();

	/**
	 * Retrieves the version information of the game.
	 *
	 * @return The version of the game (see {@link McVersion}).
	 */
	McVersion getVersion();

	/**
	 * Retrieves the entrypoint (e.g., main class or entry method) for the game.
	 *
	 * @return The entrypoint for the game as a string (the main class name).
	 */
	String getEntrypoint();

	/**
	 * Retrieves the libraries of for the game. Basically any and all libraries in
	 * the `libraries` directory, and any patched libraries in cache.
	 *
	 * @return A stream of {@link GameLibrary} objects representing the libraries for the game.
	 */
	Stream<GameLibrary> getLibraries();

	/**
	 * Retrieves the path to the launch JAR file for the game.
	 *
	 * @return The {@link Path} to the launch JAR file.
	 */
	Path getLaunchJar();

	/**
	 * Retrieves the directory where the game is launched from.
	 *
	 * @return The {@link Path} to the launch directory.
	 */
	Path getLaunchDirectory();

	/**
	 * Prepares the {@link Transformer} for class-transforming.
	 */
	void prepareTransformer();

	/**
	 * Retrieves the transformer used at runtime by the EmberClassLoader
	 *
	 * @return The {@link Transformer} instance used in the game setup.
	 */
	Transformer getTransformer();

	/**
	 * Retrieves the set of arguments used to launch the game.
	 *
	 * @return An {@link OptionSet} containing the arguments for launching the game.
	 */
	OptionSet getArguments();
}
