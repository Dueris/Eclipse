package me.dueris.eclipse;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Bootstrapper implements PluginBootstrap {

	private static Logger logger = LogManager.getLogger("EclipseBootstrap");

	public static void executePlugin(@NotNull Map<String, String> jsonData, File zipFile, File cacheDir) throws Exception {
		File jsonFile = new File("eclipse.mixin.bootstrap.json");
		try (FileWriter writer = new FileWriter(jsonFile)) {
			writer.write("{\n");
			for (Map.Entry<String, String> entry : jsonData.entrySet()) {
				writer.write("\"" + entry.getKey() + "\": \"" + entry.getValue() + "\",\n");
			}
			writer.write("}");
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
			boolean log4jFound = false;
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
					Map.of("debug", "true",
						"server_jar", "paper.jar"),
					bootstrapContext.getPluginSource().toFile(),
					bootstrapContext.getPluginSource().toAbsolutePath().getParent().getParent().resolve("cache").toFile()
				);
			} catch (Exception ea) {
				throw new RuntimeException(ea);
			}
		}
	}

	@Override
	public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
		return new EclipsePlugin();
	}

}
