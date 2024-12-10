package io.github.dueris.example;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.dueris.example.tests.TestInstance;

public class EclipseExample extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLog4JLogger().info("Loaded Eclipse example plugin!");
        TestInstance.runTests();
    }
}
