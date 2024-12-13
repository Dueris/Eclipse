package io.github.dueris.example;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.dueris.example.tests.TestInstance;

public class EclipseExample extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLog4JLogger().warn("TEST");
        getLog4JLogger().error("TEST");
        TestInstance.runTests();
        getLog4JLogger().info("Loaded Eclipse example/test plugin!");
    }
}
