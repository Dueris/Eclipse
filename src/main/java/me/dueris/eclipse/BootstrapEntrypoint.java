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
import java.util.List;
import java.util.Map;

public class BootstrapEntrypoint implements PluginBootstrap {
	private static final Logger logger = LogManager.getLogger("EclipseBootstrap");

	@SuppressWarnings("unchecked")
	public static void executePlugin(File eclipseInstance) throws Exception {
		File jsonFile = new File("eclipse.mixin.bootstrap.json");
		try (FileWriter writer = new FileWriter(jsonFile)) {
			// GSON objects automatically map to a valid Json output with toString(), so we use that for writing.

			JSONObject jsonObject = new JSONObject(Map.of(
				"ServerVersion", ServerBuildInfo.buildInfo().minecraftVersionName(),
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
		logger.info("space/vectrix/ignite/eclipse execution started, waiting for completion...");

		int exitCode = process.waitFor();
		logger.info("space/vectrix/ignite/eclipse execution completed with exit code: {}", exitCode);

		// Exit the server
		System.exit(0);
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

	@Override
	public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
		if (!IgniteBootstrap.BOOTED.get()) {
			logger.info("Starting Eclipse/Ignite bootstrap...");
			try {
				executePlugin(
					bootstrapContext.getPluginSource().toFile()
				);
			} catch (Exception ea) {
				throw new RuntimeException(ea);
			}
		}
	}

}
