package me.dueris.eclipse;

import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import space.vectrix.ignite.IgniteBootstrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BootstrapEntrypoint implements PluginBootstrap {
	private static final Logger logger = LogManager.getLogger("EclipseBootstrap");
	/**
	 * Stored for cleanup purposes
	 */
	private static final AtomicReference<Process> processRef = new AtomicReference<>();
	private static final List<Runnable> shutdownHooks = new LinkedList<>();

	public static void shutdownHook(Runnable runnable) {
		shutdownHooks.add(new Thread(runnable));
	}

	private static @NotNull List<String> buildExecutionArgs(List<String> jvmArgs, List<String> optionsetArgs, String jarPath) {
		List<String> executionArgs = new ArrayList<>();
		executionArgs.add("java");
		executionArgs.addAll(jvmArgs);
		executionArgs.add("-jar");
		executionArgs.add(jarPath);
		executionArgs.addAll(optionsetArgs);
		return executionArgs;
	}

	@SuppressWarnings("unchecked")
	public void executePlugin(File eclipseInstance) throws Exception {
		File jsonFile = new File("eclipse.mixin.bootstrap.json");
		try (FileWriter writer = new FileWriter(jsonFile)) {
			JSONObject jsonObject = new JSONObject(Map.of(
				"ServerPath", Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath()).toString(),
				"SoftwareName", ServerBuildInfo.buildInfo().brandName()
			));
			JsonObject gsonObject = new JsonObject();

			for (String key : (Iterable<String>) jsonObject.keySet()) {
				String value = jsonObject.get(key).toString();
				gsonObject.addProperty(key, value);
			}

			writer.write(gsonObject.toString());
		} catch (IOException e) {
			logger.error("Failed to create JSON file: {}", e.getMessage());
			throw e;
		}

		List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		if (!jvmArgs.isEmpty()) {
			logger.info("Located args from JVM ManagementFactory: {}", jvmArgs);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(buildExecutionArgs(jvmArgs, List.of(), eclipseInstance.getAbsolutePath())); // TODO - optionset parser - paper PR?
		processBuilder.redirectErrorStream(true); // Merge error stream with output stream
		processBuilder.inheritIO();

		Process process = processBuilder.start();
		processRef.set(process);

		try {
			exit(process.waitFor());
		} catch (InterruptedException interruptedException) {
			logger.error("Current thread, 'Eclipse-Watcher-Thread', was interrupted by another process! Exiting eclipse runtime...");
			exit(1);
		}

	}

	private void prepLaunch() {
		shutdownHook(() -> {
			try {
				Process process = processRef.get();

				if (process != null && process.isAlive()) {
					logger.info("Eclipse-Cleanup: Runtime still alive, terminating process...");
					process.destroy();
					if (!process.waitFor(5, TimeUnit.SECONDS)) {
						logger.warn("Eclipse-Cleanup: Process did not terminate gracefully, force-killing it.");
						process.destroyForcibly();
					}
					logger.info("Eclipse-Cleanup: Process terminated.");
				}

				File jsonFile = new File("eclipse.mixin.bootstrap.json");
				if (jsonFile.exists()) {
					jsonFile.delete();
				}

				logger.info("Eclipse-Cleanup: Cleanup completed.");
			} catch (InterruptedException e) {
				logger.error("Eclipse-Cleanup: Interrupted during cleanup.", e);
			} catch (Exception e) {
				logger.error("Eclipse-Cleanup: Error during cleanup.", e);
			}
		});

	}

	/**
	 * Different types of error codes: <br>
	 * - {@code   System.exit(0) or EXIT_SUCCESS;  ---> Success} <br>
	 * - {@code   System.exit(1) or EXIT_FAILURE;  ---> Exception} <br>
	 * - {@code   System.exit(-1) or EXIT_ERROR;   ---> Error} <br>
	 * Depending on the one provided, Eclipse will handle it differently
	 */
	private void exit(int exitCode) {
		if (exitCode == 0) {
			logger.info("Eclipse/Ignite process completed successfully");
		} else {
			logger.error("Eclipse/Ignite process exited with abnormal termination: {}", exitCode);
		}

		if (!shutdownHooks.isEmpty()) {
			shutdownHooks.forEach(Runnable::run);
		}
		System.exit(exitCode);
	}

	@Override
	public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
		if (!IgniteBootstrap.BOOTED.get()) {
			logger.info("Starting Eclipse/Ignite bootstrap...");
			try {
				prepLaunch();
				executePlugin(
					bootstrapContext.getPluginSource().toFile()
				);
			} catch (Exception ea) {
				throw new RuntimeException(ea);
			}
		}
	}

}
