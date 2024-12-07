package io.github.dueris.example;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public class EclipseExample extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLog4JLogger().info("Loaded Eclipse example plugin!");
    }
}
