package me.dueris.eclipse.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import me.dueris.eclipse.access.PluginClassloaderHolder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * PaperPluginParent Mixin for saving the {@link PaperPluginClassLoader} in the {@link io.papermc.paper.plugin.provider.type.paper.PaperPluginParent.PaperServerPluginProvider} to help fix plugin loading errors
 */
@Mixin(PaperPluginParent.class)
public class PaperPluginParentMixin {

	@Shadow
	@Final
	private PaperPluginClassLoader classLoader;

	@WrapMethod(method = "createPluginProvider")
	public PaperPluginParent.PaperServerPluginProvider setPluginClassloader(PaperPluginParent.PaperBootstrapProvider provider, @NotNull Operation<PaperPluginParent.PaperServerPluginProvider> original) {
		PaperPluginParent.PaperServerPluginProvider pluginProvider = original.call(provider);
		if (pluginProvider instanceof PluginClassloaderHolder holder) {
			holder.eclipse$setPluginClassLoader(classLoader);
		}
		return pluginProvider;
	}
}
