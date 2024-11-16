package me.dueris.eclipse.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import me.dueris.eclipse.EclipsePlugin;
import me.dueris.eclipse.access.MixinPlugin;
import me.dueris.eclipse.access.PluginClassloaderHolder;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import me.dueris.eclipse.ignite.api.mod.ModConfig;
import me.dueris.eclipse.ignite.api.mod.ModContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * JavaPlugin Mixin to ensure the correct classloader is being used when invoking the constructor
 */
@Mixin(JavaPlugin.class)
public abstract class JavaPluginMixin implements MixinPlugin {

	@Shadow
	@NotNull
	public abstract PluginMeta getPluginMeta();

	@ModifyExpressionValue(method = "<init>()V", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	public ClassLoader modifyInstanceof(ClassLoader original) {
		if (original instanceof ConfiguredPluginClassLoader) {
			return original;
		} else {
			PaperPluginParent.PaperServerPluginProvider runningProvider = EclipsePlugin.CURRENT_OPERATING_PROVIDER.get();
			if (runningProvider != null && runningProvider instanceof PluginClassloaderHolder holder) {
				return holder.eclipse$getPluginClassLoader();
			}

			throw new RuntimeException("Unable to locate correct class loader for plugin!");
		}
	}

	@Override
	public @Nullable ModContainer eclipse$getModContainer() {
		return IgniteBootstrap.mods().container(getPluginMeta().getName()).orElse(null);
	}

	@Override
	public @Nullable ModConfig eclipse$getModConfig() {
		ModContainer container = eclipse$getModContainer();
		if (container == null) {
			return null;
		}
		return container.config();
	}
}
