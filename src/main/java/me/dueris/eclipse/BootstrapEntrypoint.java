package me.dueris.eclipse;

import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.Main;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BootstrapEntrypoint implements PluginBootstrap {
	private static final Logger logger = LogManager.getLogger("EclipseBootstrap");

	@SuppressWarnings("unchecked")
	public static void executePlugin(File zipFile, File cacheDir) throws Exception {
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
			logger.error("Failed to create JSON file: " + e.getMessage());
			throw e;
		}

		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		File extractedJar = new File(cacheDir, "ignite.jar");
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			boolean jarFound = false;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (entry.getName().equals("ignite.jar")) {
					jarFound = true;
					extractFileFromZip(zipInputStream, extractedJar);
					logger.info("ignite.jar extracted to: " + extractedJar.getAbsolutePath());
				}
			}
			if (!jarFound) {
				throw new FileNotFoundException("ignite.jar not found in zip file");
			}
		} catch (IOException e) {
			logger.error("Failed to extract files from zip: " + e.getMessage());
			throw e;
		}

		ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", extractedJar.getAbsolutePath());
		processBuilder.redirectErrorStream(true); // Merge error stream with output stream
		processBuilder.inheritIO();

		Process process = processBuilder.start();
		logger.info("ignite/eclipse execution started, waiting for completion...");

		int exitCode = process.waitFor();
		logger.info("ignite/eclipse execution completed with exit code: " + exitCode);

		// Exit the server
		System.exit(0);
	}

	private static void extractFileFromZip(@NotNull ZipInputStream zipInputStream, File destinationFile) throws IOException {
		try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = zipInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
		}
	}

	@Override
	public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
		try {
			Class.forName("space.vectrix.ignite.IgniteBootstrap");
		} catch (Exception e) {
			logger.info("Starting Eclipse/Ignite bootstrap...");
			try {
				executePlugin(
					bootstrapContext.getPluginSource().toFile(),
					bootstrapContext.getPluginSource().toAbsolutePath().getParent().getParent().resolve("cache").toFile()
				);
			} catch (Exception ea) {
				throw new RuntimeException(ea);
			}
		}
	}

}
