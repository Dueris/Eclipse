package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dueris.eclipse.plugin.EclipsePlugin;
import io.papermc.paper.plugin.entrypoint.strategy.ProviderConfiguration;
import io.papermc.paper.plugin.entrypoint.strategy.modern.ModernPluginLoadingStrategy;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.entrypoint.DependencyContext;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ModernPluginLoadingStrategy Mixin to help resolve the issues with plugin loading /w mixin plugins
 */
@Mixin(ModernPluginLoadingStrategy.class)
public class ModernPluginLoadingStrategyMixin<T> {

	@WrapOperation(method = "loadProviders", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/entrypoint/strategy/ProviderConfiguration;applyContext(Lio/papermc/paper/plugin/provider/PluginProvider;Lio/papermc/paper/plugin/provider/entrypoint/DependencyContext;)V"))
	public void cacheCurrentProvider(ProviderConfiguration instance, PluginProvider<T> tPluginProvider, DependencyContext dependencyContext, @NotNull Operation<Void> original) {
		original.call(instance, tPluginProvider, dependencyContext);
		if (tPluginProvider instanceof PaperPluginParent.PaperServerPluginProvider pluginProvider) {
			EclipsePlugin.CURRENT_OPERATING_PROVIDER.set(pluginProvider);
		}
	}

}
