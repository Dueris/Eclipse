package io.github.dueris.example.tests;

import io.github.dueris.example.EclipseExample;
import org.bukkit.plugin.java.JavaPlugin;

public class StaticGetPluginTest implements TestInstance {

	@Override
	public void test() throws TestFailedException {
		try {
			TestInstance.LOGGER.trace("{} - JavaPlugin#getPlugin(){static} test passed", JavaPlugin.getPlugin(EclipseExample.class)
																								   .getName());
			TestInstance.LOGGER.trace("{} - JavaPlugin#getProvidingPlugin(){static} (plugin main) test passed", JavaPlugin.getProvidingPlugin(EclipseExample.class)
																														  .getName());
			TestInstance.LOGGER.trace("{} - JavaPlugin#getProvidingPlugin(){static} test passed", JavaPlugin.getProvidingPlugin(TestInstance.class)
																											.getName());
			// throw new IllegalArgumentException("TESTING"); - only uncomment when testing stacktrace logging
		} catch (Throwable throwable) {
			throw new TestFailedException("static getPlugin", throwable);
		}
	}

}
