package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.CraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftServer.class)
public class CraftServerMixin {

	@ModifyReturnValue(method = "getName", at = @At("RETURN"))
	public String eclipse$useMinecraftServer(String original) {
		return MinecraftServer.getServer().getServerModName();
	}
}
