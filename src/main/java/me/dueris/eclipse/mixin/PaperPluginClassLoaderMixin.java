package me.dueris.eclipse.mixin;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * PaperPluginClassLoader Mixin for caching the classloader impl, since we need to retrieve this later for plugin loading
 */
@Mixin(PaperPluginClassLoader.class)
public abstract class PaperPluginClassLoaderMixin extends PaperSimplePluginClassLoader implements ConfiguredPluginClassLoader {
	public PaperPluginClassLoaderMixin(Path source, JarFile file, PluginMeta configuration, ClassLoader parentLoader) throws IOException {
		super(source, file, configuration, parentLoader);
	}

}
