package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

@Mixin(targets = "io.papermc.paper.pluginremap.RemappedPluginIndex")
public class RemappedPluginIndexMixin {

	@WrapOperation(method = "getAllIfPresent", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;deleteIfExists(Ljava/nio/file/Path;)Z"))
	public boolean eclipse$no(Path path, Operation<Boolean> original) {
		return true;
	}
}
