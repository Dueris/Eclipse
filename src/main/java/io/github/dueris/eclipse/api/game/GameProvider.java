package io.github.dueris.eclipse.api.game;

import io.github.dueris.eclipse.api.GameLibrary;
import io.github.dueris.eclipse.api.McVersion;
import io.github.dueris.eclipse.api.Transformer;
import io.github.dueris.eclipse.loader.ember.patch.EmberTransformer;
import joptsimple.OptionSet;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface GameProvider {

	// Game Metadata
	String getGameId();

	String getGameName();

	McVersion getVersion();

	String getEntrypoint();

	// Game Libraries and Paths
	Stream<GameLibrary> getLibraries();

	Path getLaunchJar();

	Path getLaunchDirectory();

	// Initialization and Launch
	void prepareTransformer();

	// Transformers and Arguments
	Transformer getTransformer();

	OptionSet getArguments();
}
