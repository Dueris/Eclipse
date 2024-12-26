package io.github.dueris.eclipse.plugin.mixin;

import io.github.dueris.eclipse.api.entrypoint.BootstrapInitializer;
import io.github.dueris.eclipse.api.entrypoint.EntrypointContainer;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapMixin {

	@Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Ljava/time/Instant;now()Ljava/time/Instant;", ordinal = 0))
	private static void eclipse$injectEntrypoint(CallbackInfo ci) {
		EntrypointContainer.getEntrypoint(BootstrapInitializer.class).enter();
	}
}
