package me.dueris.eclipse.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import me.dueris.eclipse.EclipsePlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JavaPlugin.class)
public class JavaPluginMixin {

	@ModifyExpressionValue(method = "<init>()V", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	public ClassLoader modifyInstanceof(ClassLoader original) {
		if (original instanceof ConfiguredPluginClassLoader) {
			return original;
		} else {
			return EclipsePlugin.CLASSLOADERS.get(0);
		}
	}
}
