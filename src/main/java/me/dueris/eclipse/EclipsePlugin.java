package me.dueris.eclipse;

import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Do we even rly need this...? - Dueris
 */
public final class EclipsePlugin extends JavaPlugin {
	public static List<PaperSimplePluginClassLoader> CLASSLOADERS = new ArrayList<>();
	public static AtomicReference<PaperPluginParent.PaperServerPluginProvider> CURRENT_OPERATING_PROVIDER = new AtomicReference<>();
}
