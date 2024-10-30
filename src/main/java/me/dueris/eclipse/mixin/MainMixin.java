package me.dueris.eclipse.mixin;

import me.dueris.eclipse.api.entry.DedicatedServerInitEntrypoint;
import me.dueris.eclipse.api.entry.GameEntrypointManager;
import org.bukkit.craftbukkit.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

	@Inject(method = "main", at = @At("HEAD"))
	private static void executeInitEntrypoint(String[] args, CallbackInfo ci) {
		GameEntrypointManager.executeEntrypoint(DedicatedServerInitEntrypoint.class);
	}
}
