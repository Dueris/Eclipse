package io.github.dueris.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(method = "runServer", at = @At("HEAD"))
	public void example$testPrint(CallbackInfo ci) {
		System.out.println("Test! Hello World!");
	}
}
