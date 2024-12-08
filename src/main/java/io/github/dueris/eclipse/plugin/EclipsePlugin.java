package io.github.dueris.eclipse.plugin;

import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class EclipsePlugin extends JavaPlugin {
	public static final AtomicReference<PaperPluginParent.PaperServerPluginProvider> CURRENT_OPERATING_PROVIDER = new AtomicReference<>();
	public static final Map<String/*class name*/, PaperPluginParent.PaperServerPluginProvider> PLUGIN_TO_PROVIDER = new ConcurrentHashMap<>();
	public static boolean eclipse$allowsJars = false;
}
