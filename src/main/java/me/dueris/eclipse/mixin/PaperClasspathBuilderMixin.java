package me.dueris.eclipse.mixin;

import io.papermc.paper.plugin.PluginInitializerManager;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.PaperClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.library.ClassPathLibrary;
import io.papermc.paper.plugin.loader.library.PaperLibraryStore;
import me.dueris.eclipse.access.MixinPluginMeta;
import me.dueris.eclipse.ignite.agent.IgniteAgent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;

@Mixin(PaperClasspathBuilder.class)
public class PaperClasspathBuilderMixin {

	@Shadow
	@Final
	private PluginProviderContext context;

	@Inject(method = "addLibrary", at = @At("HEAD"))
	public void loadToIgniteAgent(ClassPathLibrary classPathLibrary, CallbackInfoReturnable<PluginClasspathBuilder> cir) {
		if (context.getConfiguration() instanceof MixinPluginMeta mixinPluginMeta && mixinPluginMeta.eclipse$isMixinPlugin()) {
			eclipse$buildLibraryPaths(true, classPathLibrary).forEach((path) -> {
				try {
					IgniteAgent.addJar(path);
				} catch (Throwable throwable) {
					throw new RuntimeException("Unable to append classpath library to ignite classpath!", throwable);
				}
			});
		}
	}

	@Unique
	public List<Path> eclipse$buildLibraryPaths(final boolean remap, @NotNull ClassPathLibrary library) {
		PaperLibraryStore paperLibraryStore = new PaperLibraryStore();
		library.register(paperLibraryStore);

		List<Path> paths = paperLibraryStore.getPaths();
		if (remap && PluginInitializerManager.instance().pluginRemapper != null) {
			paths = PluginInitializerManager.instance().pluginRemapper.remapLibraries(paths);
		}
		return paths;
	}
}
