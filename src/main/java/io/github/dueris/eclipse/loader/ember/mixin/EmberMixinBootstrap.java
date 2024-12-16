package io.github.dueris.eclipse.loader.ember.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.service.IMixinServiceBootstrap;

public final class EmberMixinBootstrap implements IMixinServiceBootstrap {
	public EmberMixinBootstrap() {
	}

	@Override
	public @NotNull String getName() {
		return "Ember";
	}

	@Override
	public @NotNull String getServiceClassName() {
		return "io.github.dueris.eclipse.loader.launch.ember.EmberMixinService";
	}

	@Override
	public void bootstrap() {
	}
}
