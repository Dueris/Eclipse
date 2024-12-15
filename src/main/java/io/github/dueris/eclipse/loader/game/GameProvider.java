package io.github.dueris.eclipse.loader.game;

import io.github.dueris.eclipse.loader.api.GameLibrary;
import io.github.dueris.eclipse.loader.api.McVersion;
import io.github.dueris.eclipse.loader.launch.EmberLauncher;
import joptsimple.OptionSet;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface GameProvider {
	Stream<GameLibrary> getLibraries();

	Path getLaunchJar();

	String getGameId();

	String getGameName();

	McVersion getVersion();

	String getEntrypoint();

	Path getLaunchDirectory();

	void initialize(EmberLauncher launcher);

	GameTransformer getEntrypointTransformer();

	void launch(ClassLoader loader);

	OptionSet getArguments();
}
