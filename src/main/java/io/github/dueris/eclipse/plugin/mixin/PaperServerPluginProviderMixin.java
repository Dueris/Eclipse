package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dueris.eclipse.plugin.EclipsePlugin;
import io.github.dueris.eclipse.plugin.access.PluginClassloaderHolder;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PaperServerPluginProvider Mixin for caching/getting the plugin classloader
 */
@Mixin(PaperPluginParent.PaperServerPluginProvider.class)
public class PaperServerPluginProviderMixin implements PluginClassloaderHolder {

	@Unique
	private PaperPluginClassLoader eclipse$paperPluginClassLoader;

	@Override
	public PaperPluginClassLoader eclipse$getPluginClassLoader() {
		return eclipse$paperPluginClassLoader;
	}

	@Override
	public void eclipse$setPluginClassLoader(PaperPluginClassLoader loader) {
		eclipse$paperPluginClassLoader = loader;
	}

	@WrapOperation(method = "createInstance()Lorg/bukkit/plugin/java/JavaPlugin;", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/provider/configuration/PaperPluginMeta;getMainClass()Ljava/lang/String;", ordinal = 0))
	public String eclipse$storePlugin(PaperPluginMeta instance, @NotNull Operation<String> original) {
		String main = original.call(instance);
		EclipsePlugin.PLUGIN_TO_PROVIDER.put(main, (PaperPluginParent.PaperServerPluginProvider) (Object) this);
		return main;
	}
}
