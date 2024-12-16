package io.github.dueris.eclipse.api.mod;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.jar.Manifest;

@ApiStatus.NonExtendable
public interface ModResource {
	@NotNull String locator();

	@NotNull Path path();

	@UnknownNullability
	Manifest manifest();

	@NotNull FileSystem fileSystem();
}
