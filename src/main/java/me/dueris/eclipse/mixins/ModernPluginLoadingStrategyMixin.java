package me.dueris.eclipse.mixins;

import com.google.common.collect.Maps;
import com.google.common.graph.GraphBuilder;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.dependency.MetaDependencyTree;
import io.papermc.paper.plugin.entrypoint.strategy.ProviderConfiguration;
import io.papermc.paper.plugin.entrypoint.strategy.ProviderLoadingStrategy;
import io.papermc.paper.plugin.entrypoint.strategy.modern.ModernPluginLoadingStrategy;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import me.dueris.eclipse.plugin.EclipseLoadOrderTree;
import me.dueris.eclipse.plugin.PluginProcessAccessors;
import me.dueris.eclipse.plugin.PluginProviderEntry;
import org.bukkit.plugin.UnknownDependencyException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ModernPluginLoadingStrategy.class)
public class ModernPluginLoadingStrategyMixin<T> {

	@Shadow @Final private static Logger LOGGER;

	@Shadow @Final private ProviderConfiguration<T> configuration;

	/**
	 * @author Dueris
	 * @reason We need to make changes to how plugins are loaded to prevent things like Eclipse loading again.
	 */
	@Overwrite
	public List<ProviderLoadingStrategy.ProviderPair<T>> loadProviders(@NotNull List<PluginProvider<T>> pluginProviders, MetaDependencyTree dependencyTree) {
		Map<String, PluginProviderEntry<T>> providerMap = new HashMap<>();
		Map<String, PluginProvider<?>> providerMapMirror = Maps.transformValues(providerMap, (entry) -> entry.getProvider());
		List<PluginProvider<T>> validatedProviders = new ArrayList<>();

		for (PluginProvider<T> provider : pluginProviders) {
			PluginMeta providerConfig = provider.getMeta();
			PluginProviderEntry<T> entry = new PluginProviderEntry<>(provider);

			PluginProviderEntry<T> replacedProvider = providerMap.put(providerConfig.getName(), entry);
			if (replacedProvider != null) {
				LOGGER.error(String.format(
					"Ambiguous plugin name '%s' for files '%s' and '%s' in '%s'",
					providerConfig.getName(),
					provider.getSource(),
					replacedProvider.getProvider().getSource(),
					replacedProvider.getProvider().getParentSource()
				));
				this.configuration.onGenericError(replacedProvider.getProvider());
			}

			for (String extra : providerConfig.getProvidedPlugins()) {
				PluginProviderEntry<T> replacedExtraProvider = providerMap.putIfAbsent(extra, entry);
				if (replacedExtraProvider != null) {
					LOGGER.warn(String.format(
						"`%s' is provided by both `%s' and `%s'",
						extra,
						providerConfig.getName(),
						replacedExtraProvider.getProvider().getMeta().getName()
					));
				}
			}
		}

		for (PluginProvider<?> validated : pluginProviders) {
			dependencyTree.add(validated);
		}

		for (PluginProvider<T> provider : pluginProviders) {
			PluginMeta configuration = provider.getMeta();

			List<String> missingDependencies = provider.validateDependencies(dependencyTree);

			if (missingDependencies.isEmpty()) {
				validatedProviders.add(provider);
			} else {
				LOGGER.error("Could not load '%s' in '%s'".formatted(provider.getSource(), provider.getParentSource()), new UnknownDependencyException(missingDependencies, configuration.getName())); // Paper
				providerMap.remove(configuration.getName());
				dependencyTree.remove(provider);
				this.configuration.onGenericError(provider);
			}
		}

		EclipseLoadOrderTree eclipseLoaderTree = new EclipseLoadOrderTree(providerMapMirror, GraphBuilder.directed().build());
		for (PluginProvider<?> validated : validatedProviders) {
			eclipseLoaderTree.add(validated);
		}

		List<String> reversedTopographicSort = eclipseLoaderTree.getLoadOrder();
		List<ProviderLoadingStrategy.ProviderPair<T>> loadedPlugins = new ArrayList<>();
		for (String providerIdentifier : reversedTopographicSort) {
			PluginProviderEntry<T> retrievedProviderEntry = providerMap.get(providerIdentifier);
			if (retrievedProviderEntry == null || retrievedProviderEntry.isProvided()) {
				continue;
			}
			retrievedProviderEntry.setProvided(true);
			PluginProvider<T> retrievedProvider = retrievedProviderEntry.getProvider();
			try {
				this.configuration.applyContext(retrievedProvider, dependencyTree);

				if (this.configuration.preloadProvider(retrievedProvider)) {
					if (retrievedProvider instanceof PaperPluginParent.PaperServerPluginProvider pluginProvider) {
						PluginProcessAccessors.CURRENT_OPERATING_PROVIDER.set(pluginProvider);
					}
					T instance = retrievedProvider.createInstance();
					if (this.configuration.load(retrievedProvider, instance)) {
						loadedPlugins.add(new ProviderLoadingStrategy.ProviderPair<>(retrievedProvider, instance));
					}
				}
			} catch (Throwable ex) {
				LOGGER.error("Could not load plugin '%s' in folder '%s'".formatted(retrievedProvider.getFileName(), retrievedProvider.getParentSource()), ex); // Paper
			}
		}

		return loadedPlugins;
	}
}
