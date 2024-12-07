package io.github.dueris.eclipse.plugin.mixin;

import io.papermc.paper.plugin.PluginInitializerManager;
import io.papermc.paper.plugin.provider.source.DirectoryProviderSource;
import io.papermc.paper.plugin.util.EntrypointUtil;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Paths;

@Mixin(PluginInitializerManager.class)
public class PluginInitializerManagerMixin {

	@Shadow
	@Final
	private static Logger LOGGER;

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/util/EntrypointUtil;registerProvidersFromSource(Lio/papermc/paper/plugin/provider/source/ProviderSource;Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
	private static void eclipse$loadProcessedPlugins(OptionSet optionSet, CallbackInfo ci) {
		LOGGER.info("Loading processed mods into plugin entrypoints...");
		EntrypointUtil.registerProvidersFromSource(DirectoryProviderSource.INSTANCE, Paths.get(".").toAbsolutePath().resolve("cache").resolve(".eclipse").resolve("processedMods"));
	}
}
