package io.github.dueris.eclipse.plugin.mixin;

import io.github.dueris.eclipse.loader.api.entrypoint.BootstrapInitializer;
import io.github.dueris.eclipse.loader.entrypoint.EntrypointContainer;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapMixin {

	@Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Ljava/time/Instant;now()Ljava/time/Instant;", shift = At.Shift.AFTER))
	private static void eclipse$enterBootstrapEntrypoint(CallbackInfo ci) {
		EntrypointContainer.getEntrypoint(BootstrapInitializer.class).enter();
	}
}
