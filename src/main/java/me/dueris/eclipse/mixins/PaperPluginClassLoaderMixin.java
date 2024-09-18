package me.dueris.eclipse.mixins;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import me.dueris.eclipse.EclipsePlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.logging.Logger;

@Mixin(PaperPluginClassLoader.class)
public abstract class PaperPluginClassLoaderMixin extends PaperSimplePluginClassLoader implements ConfiguredPluginClassLoader {
	public PaperPluginClassLoaderMixin(Path source, JarFile file, PluginMeta configuration, ClassLoader parentLoader) throws IOException {
		super(source, file, configuration, parentLoader);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void loadEclipseClassloaderImpl(Logger logger, Path source, JarFile file, PaperPluginMeta configuration, ClassLoader parentLoader, URLClassLoader libraryLoader, CallbackInfo ci) {
		EclipsePlugin.CLASSLOADERS.add(this);
	}
}
