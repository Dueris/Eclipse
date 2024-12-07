package io.github.dueris.eclipse.loader.api.mod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.TaggedLogger;

import java.util.Objects;

/**
 * Represents a mod container.
 *
 * @author vectrix
 * @since 1.0.0
 */
public record ModContainerImpl(TaggedLogger logger, ModResource resource, ModMetadata config) implements ModContainer {

	@Override
	public @NotNull String id() {
		return this.config.id();
	}

	@Override
	public @NotNull String version() {
		return this.config.version();
	}

	@Override
	public @NotNull ModMetadata config() {
		return this.config;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.resource, this.config);
	}

	@Override
	public boolean equals(final @Nullable Object other) {
		if (this == other) return true;
		if (!(other instanceof ModContainerImpl that)) return false;
		return Objects.equals(this.resource, that.resource)
			&& Objects.equals(this.config, that.config);
	}

	@Override
	public @NotNull String toString() {
		return "ModContainerImpl(id=" + this.id() + ", version=" + this.version() + ", resource=" + this.resource() + ", config=" + this.config() + ")";
	}
}
