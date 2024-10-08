package me.dueris.eclipse.mixins;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import me.dueris.eclipse.access.PluginClassloaderHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
}
