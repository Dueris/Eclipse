package me.dueris.eclipse.util;

import com.dragoncommissions.mixbukkit.MixBukkit;
import com.dragoncommissions.mixbukkit.MixinPluginInstance;
import com.dragoncommissions.mixbukkit.addons.AutoMapper;
import com.dragoncommissions.mixbukkit.api.MixinPlugin;
import com.dragoncommissions.mixbukkit.api.action.impl.MActionInsertShellCode;
import com.dragoncommissions.mixbukkit.api.locator.impl.HLocatorHead;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.CallbackInfo;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.ShellCodeReflectionMixinPluginMethodCall;
import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import joptsimple.OptionSet;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class BootstrapEntrypoint implements PluginBootstrap {
	protected static final Logger logger = LogManager.getLogger("EclipseBootstrap");
	/**
	 * Stored for cleanup purposes
	 */
	private static final AtomicReference<Process> processRef = new AtomicReference<>();
	private static final List<Runnable> shutdownHooks = new LinkedList<>();
	protected static BootstrapContext CONTEXT;

	public static void shutdownHook(Runnable runnable) {
		shutdownHooks.add(new Thread(runnable));
	}

	private static @NotNull List<String> buildExecutionArgs(List<String> jvmArgs, String jarPath) {
		List<String> executionArgs = new ArrayList<>();
		Optional<String> commandPath = ProcessHandle.current().info().command();
		executionArgs.add(commandPath.orElseThrow());
		executionArgs.addAll(jvmArgs);
		executionArgs.add("-jar");
		executionArgs.add(jarPath);
		return executionArgs;
	}

	public static @Nullable Thread getThreadById(long threadId) {
		Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();

		for (Thread thread : allThreads.keySet()) {
			if (thread.threadId() == threadId) {
				return thread;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected void executePlugin(File eclipseInstance, OptionSet optionSet) throws Exception {
		File jsonFile = new File("eclipse.mixin.bootstrap.json");
		try (FileWriter writer = new FileWriter(jsonFile)) {
			JSONObject jsonObject = new JSONObject(Map.of(
				"ServerPath", Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath()).toString(),
				"SoftwareName", ServerBuildInfo.buildInfo().brandName(),
				"OptionSet", OptionSetStringSerializer.serializeOptionSetFields(optionSet)
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

		ProcessBuilder processBuilder = new ProcessBuilder(buildExecutionArgs(jvmArgs, eclipseInstance.getAbsolutePath()));
		processBuilder.redirectErrorStream(true); // Merge error stream with output stream
		processBuilder.inheritIO();

		Process process = processBuilder.start();
		processRef.set(process);

		try {
			// Block current server process from doing anything, as we are currently in control now.
			exit(process.waitFor());
		} catch (InterruptedException interruptedException) {
			logger.error("Current thread, 'Eclipse-Watcher-Thread', was interrupted by another process! Exiting eclipse runtime...");
			exit(1);
		}

	}

	protected void prepLaunch() {
		for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
			ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(id);
			if (info.getThreadName().startsWith("Paper Plugin Remapper")) {
				Thread thread = getThreadById(id);
				if (thread != null) thread.interrupt();
			}
		}
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
			CONTEXT = bootstrapContext;
			MixBukkit bukkit = new MixBukkit((PaperPluginClassLoader) BootstrapEntrypoint.class.getClassLoader());
			bukkit.onEnable(logger, bootstrapContext.getPluginSource().toFile());
			MixinPlugin mixinPlugin = bukkit.registerMixinPlugin(new MixinPluginInstance("Eclipse"), AutoMapper.getMappingAsStream());
			try {
				mixinPlugin.registerMixin(
					"eclipseInitWrap", new MActionInsertShellCode(
						new ShellCodeReflectionMixinPluginMethodCall(Injectors.class.getDeclaredMethod("eclipseLoadWrapper", Path.class, OptionSet.class, CallbackInfo.class)),
						new HLocatorHead()
					), DedicatedServerProperties.class, "fromFile", DedicatedServerProperties.class.getDeclaredMethod("fromFile", Path.class, OptionSet.class).getReturnType(),
					DedicatedServerProperties.class.getDeclaredMethod("fromFile", Path.class, OptionSet.class).getParameterTypes()
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

}
