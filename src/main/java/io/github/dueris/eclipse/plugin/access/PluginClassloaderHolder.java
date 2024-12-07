package io.github.dueris.eclipse.plugin.access;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

public interface PluginClassloaderHolder {
	PaperPluginClassLoader eclipse$getPluginClassLoader();

	void eclipse$setPluginClassLoader(PaperPluginClassLoader loader);
}
