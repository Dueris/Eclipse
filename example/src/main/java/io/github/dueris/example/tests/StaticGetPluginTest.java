package io.github.dueris.example.tests;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.dueris.example.EclipseExample;

public class StaticGetPluginTest implements TestInstance {

    @Override
    public void test() throws TestFailedException {
        try {
            TestInstance.LOGGER.trace("{} - JavaPlugin#getPlugin(){static} test passed", JavaPlugin.getPlugin(EclipseExample.class).getName());
        } catch (Throwable throwable) {
            throw new TestFailedException("static getPlugin", throwable);
        }
    }
    
}
