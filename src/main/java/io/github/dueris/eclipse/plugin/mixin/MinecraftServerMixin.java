package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.dueris.eclipse.loader.util.Util;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@ModifyReturnValue(method = "getServerModName", at = @At("RETURN"))
	public String eclipse$modifyBranding(String original) {
		return Util.insertBranding(original);
	}
}
