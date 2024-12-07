package io.github.dueris.eclipse.loader.api.mod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.Manifest;

/**
 * Represents a mod resource that may not be resolved.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class ModResourceImpl implements ModResource {
	private final String locator;
	private final Path path;
	private final Manifest manifest;
	private final boolean child;
	private final List<ModResource> children;

	private FileSystem fileSystem;

	ModResourceImpl(final @NotNull String locator,
					final @NotNull Path path,
					final @UnknownNullability Manifest manifest, boolean child, List<ModResource> children) {
		this.locator = locator;
		this.path = path;
		this.manifest = manifest;
		this.child = child;
		this.children = children;
	}

	@Override
	public @NotNull String locator() {
		return this.locator;
	}

	@Override
	public @NotNull Path path() {
		return this.path;
	}

	@Override
	public @UnknownNullability Manifest manifest() {
		return this.manifest;
	}

	@Override
	public @NotNull FileSystem fileSystem() {
		if (this.fileSystem == null) {
			try {
				this.fileSystem = FileSystems.newFileSystem(this.path(), this.getClass().getClassLoader());
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}

		return this.fileSystem;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.locator, this.path, this.manifest);
	}

	@Override
	public boolean equals(final @Nullable Object other) {
		if (this == other) return true;
		if (!(other instanceof ModResourceImpl that)) return false;
		return Objects.equals(path.toAbsolutePath().normalize(), that.path.toAbsolutePath().normalize());
	}

	@Override
	public @NotNull String toString() {
		return "ModResourceImpl{locator='" + this.locator + ", path=" + this.path + ", manifest=" + this.manifest + "}";
	}

	public boolean isChild() {
		return child;
	}

	public List<ModResource> getChildren() {
		return children;
	}
}
