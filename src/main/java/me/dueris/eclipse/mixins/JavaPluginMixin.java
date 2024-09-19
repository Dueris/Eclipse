package me.dueris.eclipse.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import me.dueris.eclipse.EclipsePlugin;
import me.dueris.eclipse.plugin.PluginClassloaderHolder;
import me.dueris.eclipse.plugin.PluginProcessAccessors;
import net.minecraft.world.entity.EntityType;
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
			PaperPluginParent.PaperServerPluginProvider runningProvider = PluginProcessAccessors.CURRENT_OPERATING_PROVIDER.get();
			if (runningProvider != null && runningProvider instanceof PluginClassloaderHolder holder) {
				return holder.eclipse$getPluginClassLoader();
			}

			for (PaperSimplePluginClassLoader classloader : EclipsePlugin.CLASSLOADERS) {
				try {
					classloader.loadClass(this.getClass().getName());
					return classloader;
				} catch (ClassNotFoundException ignored) {
				}
			}

			throw new RuntimeException("Unable to locate correct class loader for plugin!");
		}
	}
}
