package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.api.mod.ModContainer;
import io.github.dueris.eclipse.loader.api.impl.ModMetadata;
import io.github.dueris.eclipse.plugin.EclipsePlugin;
import io.github.dueris.eclipse.plugin.access.MixinPlugin;
import io.github.dueris.eclipse.plugin.access.PluginClassloaderHolder;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * JavaPlugin Mixin to ensure the correct classloader is being used when invoking the constructor
 */
@Mixin(JavaPlugin.class)
public abstract class JavaPluginMixin implements MixinPlugin {

	@Inject(method = "getPlugin", at = @At("HEAD"))
	private static <T extends JavaPlugin> void storeGetting(@NotNull Class<T> clazz, CallbackInfoReturnable<T> cir, @Share("fetchingPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		fetchingPlugin.set(clazz.getName());
	}

	@ModifyExpressionValue(method = "getPlugin", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	private static ClassLoader returnCorrectClassloader(ClassLoader original, @Share("fetchingPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		return ((PluginClassloaderHolder) EclipsePlugin.PLUGIN_TO_PROVIDER.get(fetchingPlugin.get())).eclipse$getPluginClassLoader();
	}

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

			throw new RuntimeException("Unable to locate correct class loader for plugin! Found: " + original.getClass().getName());
		}
	}

	@Override
	public @Nullable ModContainer eclipse$getModContainer() {
		return EclipseLoaderBootstrap.mods().container(getPluginMeta().getName()).orElse(null);
	}

	@Override
	public @Nullable ModMetadata eclipse$getModConfig() {
		ModContainer container = eclipse$getModContainer();
		if (container == null) {
			return null;
		}
		return container.config();
	}

}
