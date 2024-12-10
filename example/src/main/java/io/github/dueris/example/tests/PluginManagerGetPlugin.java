package io.github.dueris.example.tests;

import org.bukkit.Bukkit;

public class PluginManagerGetPlugin implements TestInstance {

    @Override
    public void test() throws TestFailedException {
        try {
            TestInstance.LOGGER.trace("{} - Bukkit.getPluginManager()#getPlugin() test passed", Bukkit.getPluginManager().getPlugin("EclipseTest").getName());
        } catch (Throwable throwable) {
            throw new TestFailedException("static getPlugin", throwable);
        }
    }
    
}
