package io.github.dueris.eclipse.loader.ember.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;

import java.nio.file.Path;
import java.util.Map;

public final class EmberMixinContainer extends ContainerHandleVirtual {
	public EmberMixinContainer(final @NotNull String name) {
		super(name);
	}

	public void addResource(final @NotNull String name, final @NotNull Path path) {
		this.add(new ResourceContainer(name, path));
	}

	public void addResource(final Map.@NotNull Entry<String, Path> entry) {
		this.add(new ResourceContainer(entry.getKey(), entry.getValue()));
	}

	@Override
	public @NotNull String toString() {
		return "EmberMixinContainer{name=" + this.getName() + "}";
	}

	static class ResourceContainer extends ContainerHandleURI {
		private final String name;
		private final Path path;

		ResourceContainer(final @NotNull String name, final @NotNull Path path) {
			super(path.toUri());

			this.name = name;
			this.path = path;
		}

		public @NotNull String name() {
			return this.name;
		}

		public @NotNull Path path() {
			return this.path;
		}

		@Override
		public @NotNull String toString() {
			return "ResourceContainer{name=" + this.name + ", path=" + this.path + "}";
		}
	}
}
