package me.dueris.eclipse.plugin;

import io.papermc.paper.plugin.provider.PluginProvider;

public class PluginProviderEntry<T> {

	private final PluginProvider<T> provider;
	private boolean provided;

	public PluginProviderEntry(PluginProvider<T> provider) {
		this.provider = provider;
	}

	public PluginProvider<T> getProvider() {
		return provider;
	}

	public boolean isProvided() {
		return provided;
	}

	public void setProvided(boolean provided) {
		this.provided = provided;
	}
}
