package io.github.dueris.example.tests;

import java.util.List;

import org.apache.logging.log4j.*;

import io.github.dueris.example.EclipseExample;

public interface TestInstance {
    public static Logger LOGGER = LogManager.getLogger("EclipseTests");
    void test() throws TestFailedException;

    static void runTests() {
       for (Class<? extends TestInstance> testClass : List.of(StaticGetPluginTest.class, PluginManagerGetPlugin.class, ColoredLoggingOutputTest.class)) {
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
}
