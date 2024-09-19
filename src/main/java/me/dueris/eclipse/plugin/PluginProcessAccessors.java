package me.dueris.eclipse.plugin;

import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;

import java.util.concurrent.atomic.AtomicReference;

public class PluginProcessAccessors {
	public static AtomicReference<PaperPluginParent.PaperServerPluginProvider> CURRENT_OPERATING_PROVIDER = new AtomicReference<>();
}
