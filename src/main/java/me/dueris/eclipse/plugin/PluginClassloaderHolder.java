package me.dueris.eclipse.plugin;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

public interface PluginClassloaderHolder {
	PaperPluginClassLoader eclipse$getPluginClassLoader();

	void eclipse$setPluginClassLoader(PaperPluginClassLoader loader);
}
