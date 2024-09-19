package me.dueris.eclipse;

import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Do we even rly need this...? - Dueris
 */
public final class EclipsePlugin extends JavaPlugin {

	public static List<PaperSimplePluginClassLoader> CLASSLOADERS = new ArrayList<>();

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
	}
}
