package io.github.dueris.example.tests;

import io.github.dueris.example.EclipseExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public interface TestInstance {
	Logger LOGGER = LogManager.getLogger("EclipseTests");
	static void runTests() {
		for (Class<? extends TestInstance> testClass : List.of(StaticGetPluginTest.class, PluginManagerGetPlugin.class, ColoredLoggingOutputTest.class, JavaPluginJarFileTest.class)) {
			try {
				TestInstance testInstance = testClass.newInstance();
				try {
					testInstance.test();
				} catch (TestFailedException throwable) {
					throw new RuntimeException("Test failed! : " + testClass.getName(), throwable);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException("Unable to create new test instance!", e);
			}
		}
		EclipseExample.getPlugin(EclipseExample.class).getLog4JLogger().info("All tests passed!");
	}
	void test() throws TestFailedException;
}
