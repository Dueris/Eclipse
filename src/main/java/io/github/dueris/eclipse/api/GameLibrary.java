package io.github.dueris.eclipse.api;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record GameLibrary(Path libraryPath, String libraryString, boolean trace) {

	@Override
	public @NotNull String toString() {
		return "GameLibrary[" +
			"libraryPath=" + libraryPath + ", " +
			"libraryString=" + libraryString + ']';
	}

}
