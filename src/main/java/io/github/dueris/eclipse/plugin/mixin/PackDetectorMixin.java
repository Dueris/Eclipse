package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.packs.repository.PackDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static io.github.dueris.eclipse.plugin.EclipsePlugin.eclipse$allowsJars;

@Mixin(PackDetector.class)
public class PackDetectorMixin {

	@WrapOperation(method = "detectPackResources", at = @At(value = "INVOKE", target = "Ljava/lang/String;endsWith(Ljava/lang/String;)Z"))
	public boolean eclipse$allowJars(String instance, String suffix, Operation<Boolean> original) {
		if (eclipse$allowsJars && suffix.equalsIgnoreCase(".zip")) {
			return original.call(instance, ".jar");
		}
		return original.call(instance, suffix);
	}

	// Basically ignore any directories during jar analysis
	@ModifyExpressionValue(method = "detectPackResources", at = @At(value = "INVOKE", target = "Ljava/nio/file/attribute/BasicFileAttributes;isDirectory()Z"))
	public boolean eclipse$ignoreDirectories(boolean original) {
		if (original && eclipse$allowsJars) {
			return false;
		}
		return original;
	}
}
