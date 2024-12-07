package io.github.dueris.eclipse.loader.game;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record GameLibrary(Path libraryPath, String libraryString) {

	@Override
	public @NotNull String toString() {
		return "GameLibrary[" +
			"libraryPath=" + libraryPath + ", " +
			"libraryString=" + libraryString + ']';
	}

}
