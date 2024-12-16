package io.github.dueris.eclipse.plugin.util;

import com.dragoncommissions.mixbukkit.MixBukkit;
import com.dragoncommissions.mixbukkit.MixinPluginInstance;
import com.dragoncommissions.mixbukkit.addons.AutoMapper;
import com.dragoncommissions.mixbukkit.api.MixinPlugin;
import com.dragoncommissions.mixbukkit.api.action.impl.MActionInsertShellCode;
import com.dragoncommissions.mixbukkit.api.locator.impl.HLocatorHead;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.CallbackInfo;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.ShellCodeReflectionMixinPluginMethodCall;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dueris.eclipse.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.Main;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import joptsimple.OptionSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Bootstrap/pre-boot stage, setup for ignite context
 */
@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class BootstrapEntrypoint implements PluginBootstrap {
	protected static final Logger logger = LogManager.getLogger("EclipseBootstrap");
	/**
	 * Stored for cleanup purposes
	 */
	private static final AtomicReference<Process> processRef = new AtomicReference<>();
	private static final List<Runnable> shutdownHooks = new LinkedList<>();
	protected static BootstrapContext CONTEXT;
	private static boolean PROVIDER_CONTEXT = false;
	private Function<Component, String> serializer;

	public static void shutdownHook(Runnable runnable) {
		shutdownHooks.add(new Thread(runnable));
	}

	/**
	 * Builds execution arguments for starting a new {@link Process}
	 */
	private static @NotNull List<String> buildExecutionArgs(List<String> jvmArgs, String jarPath) {
		List<String> executionArgs = new ArrayList<>();
		Optional<String> commandPath = ProcessHandle.current().info().command();
		executionArgs.add(commandPath.orElseThrow());
		executionArgs.addAll(jvmArgs);
		executionArgs.add("-jar");
		executionArgs.add(jarPath);
		return executionArgs;
	}

	/**
	 * Searches for a thread with the id matching the id provided
	 *
	 * @param threadId the id of the thread to search for
	 * @return the thread found, if null then no thread matching the id exists
	 */
	public static @Nullable Thread getThreadById(long threadId) {
		Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();

		for (Thread thread : allThreads.keySet()) {
			if (thread.threadId() == threadId) {
				return thread;
			}
		}

		return null;
	}

	/**
	 * Starts ignite process and transforms current thread into a watcher and listener for mirroring
	 */
	protected void executePlugin(File eclipseInstance, OptionSet optionSet) throws Exception {
		File contextFile = new File(Paths.get(".").resolve("cache").resolve(".eclipse").toFile(), "bootstrap.context");
		if (!contextFile.getParentFile().exists()) {
			contextFile.getParentFile().mkdirs();
		}
		try (FileWriter writer = new FileWriter(contextFile)) {
			JsonObject gsonObject = new JsonObject();
			gsonObject.addProperty("path", Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath())
												.toAbsolutePath().normalize().toString());
			gsonObject.addProperty("brand", ServerBuildInfo.buildInfo().brandName());
			gsonObject.addProperty("is_provider_context", PROVIDER_CONTEXT);

			JsonArray jsonArray = new JsonArray();
			for (byte b : new SerializedOptionSetData().serialize(optionSet).compileBytes()) {
				jsonArray.add(b);
			}
			gsonObject.add("optionset", jsonArray);
			writer.write(IgniteConstants.GSON.toJson(gsonObject));
		} catch (IOException e) {
			logger.error("Failed to create context file: {}", e.getMessage());
			throw e;
		}

		List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		if (!jvmArgs.isEmpty()) {
			logger.info("Located args from JVM ManagementFactory: {}", jvmArgs);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(buildExecutionArgs(jvmArgs, eclipseInstance.getAbsolutePath()));
		{
			// We use kyori component logger to grab its serializer, which translates a Component -> String, including its custom colors
			ComponentLogger componentLogger = ComponentLogger.logger("Eclipse");
			Field serializerField = componentLogger.getClass().getDeclaredField("serializer");
			serializerField.setAccessible(true);
			//noinspection unchecked
			serializer = (Function<Component, String>) serializerField.get(componentLogger);
		}

		Process process = processBuilder.start();
		processRef.set(process);

		Thread inHandler = inputThread(process);
		Thread outHandler = outThread(process.getInputStream(), System.out, "Error reading process output: ", "ProcessOutStream");
		Thread errHandler = outThread(process.getErrorStream(), System.err, "Error reading process error stream: ", "ProcessErrStream");

		Thread.currentThread().setName("Eclipse-Watcher");
		try {
			int exitCode = process.waitFor();
			inHandler.interrupt();
			outHandler.interrupt();
			errHandler.interrupt();
			exit(exitCode);
		} catch (InterruptedException e) {
			checkKillProcess();
			exit(1);
		}

	}

	private @NotNull Thread outThread(InputStream process, PrintStream out, String x, String ProcessOutStream) {
		Thread outputHandler = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process))) {
				String line;
				Component lineComponent;
				while ((line = reader.readLine()) != null) {
					if (line.matches("^\\[.*?]\\s\\[.*?/WARN]:.*")) {
						lineComponent = warnComponent(line);
					} else if (line.matches("^\\[.*?]\\s\\[.*?/ERROR]:.*")
						|| (line.matches("^\\s+at\\s+(.*?)?[\\w.$_]+\\.[\\w$<>]+\\((.*?:\\d+|Native Method)\\)(\\s~\\[.*])?")
						|| line.matches("^(?!\\[\\d{2}:\\d{2}:\\d{2}] \\[[^]]+/[A-Za-z]+]:)[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*:.*$")
						|| line.matches("^Caused by: [a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*:.*$")
						|| line.matches("^\\s*\\.\\.\\. \\d+ more$")
						|| line.matches("^Exception in thread \".*?\" [a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*: .*$"))) {
						lineComponent = errorComponent(line);
					} else {
						lineComponent = Component.text(line);
					}
					printLine(lineComponent, out);
				}
			} catch (IOException e) {
				printLine(errorComponent(x + e.getMessage()), System.err);
			}
		}, ProcessOutStream);

		outputHandler.start();
		return outputHandler;
	}

	private @NotNull Thread inputThread(Process process) {
		Thread inputHandler = new Thread(() -> {
			OutputStream out = process.getOutputStream();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
				String line;
				while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
					writer.write(line);
					writer.newLine();
					writer.flush();
				}
			} catch (IOException e) {
				if (!Thread.currentThread().isInterrupted()) {
					printLine(errorComponent("Input handler encountered an I/O error: " + e.getMessage()), System.err);
				}
			}
		}, "ProcessInStream");

		inputHandler.start();
		return inputHandler;
	}

	/**
	 * Prepares the launch context
	 */
	protected void prepLaunch() {
		if (Boolean.getBoolean("eclipse.isprovidercontext")) {
			System.getProperties().remove("eclipse.isprovidercontext");
			PROVIDER_CONTEXT = true;
		}
		for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
			ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(id);
			if (info != null && info.getThreadName().startsWith("Paper Plugin Remapper")) {
				Thread thread = getThreadById(id);
				if (thread != null) thread.interrupt();
			}
		}
		shutdownHook(() -> {
			try {
				checkKillProcess();
			} catch (InterruptedException e) {
				System.err.println("Eclipse-Cleanup: Interrupted during cleanup." + e);
			} catch (Exception e) {
				System.err.println("Eclipse-Cleanup: Error during cleanup." + e);
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
		if (!shutdownHooks.isEmpty()) {
			shutdownHooks.forEach(Runnable::run);
		}
		printLine(Component.text("Exiting Minecraft server via Eclipse with exit code " + exitCode), System.out);
		System.exit(exitCode);
	}

	private void printLine(Component line, @NotNull PrintStream stream) {
		// We have to run it through "print" because "println" is overrided by Papers
		// "WrappedOutStream" class, which appends with the plugin logger
		stream.print(serializer.apply(line));
		stream.print("\n");
	}

	private @NotNull Component errorComponent(String string) {
		return Component.text(string).color(TextColor.color(0xE74856));
	}

	private @NotNull Component warnComponent(String string) {
		return Component.text(string).color(TextColor.color(0xF9F2A1));
	}

	/**
	 * Setup for pre-boot of ignite process, register hard-coded jvm modifier for init
	 *
	 * @param bootstrapContext the server provided context
	 */
	@Override
	public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
		if (!Main.BOOTED.get()) {
			CONTEXT = bootstrapContext;
			MixBukkit bukkit = new MixBukkit((PaperPluginClassLoader) BootstrapEntrypoint.class.getClassLoader());
			bukkit.onEnable(logger, bootstrapContext.getPluginSource().toFile());
			MixinPlugin mixinPlugin = bukkit.registerMixinPlugin(new MixinPluginInstance("Eclipse"), AutoMapper.getMappingAsStream());
			try {
				mixinPlugin.registerMixin(
					"eclipseInitWrap", new MActionInsertShellCode(
						new ShellCodeReflectionMixinPluginMethodCall(Injectors.class.getDeclaredMethod("eclipseLoadWrapper", Path.class, OptionSet.class, CallbackInfo.class)),
						new HLocatorHead()
					), DedicatedServerProperties.class, "fromFile", DedicatedServerProperties.class.getDeclaredMethod("fromFile", Path.class, OptionSet.class)
																								   .getReturnType(),
					DedicatedServerProperties.class.getDeclaredMethod("fromFile", Path.class, OptionSet.class)
												   .getParameterTypes()
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Ensures the program is dead, and if it isnt then we politely ask to stop,
	 * after 45 seconds we kill it
	 */
	public void checkKillProcess() throws InterruptedException {
		Process process = processRef.get();

		if (process != null && process.isAlive()) {
			logger.info("Eclipse-Cleanup: Runtime still alive, terminating process...");

			CompletableFuture<Void> serverExitFuture = new CompletableFuture<>();

			process.onExit().thenRun(() -> {
				logger.info("Eclipse-Cleanup: Process terminated successfully.");
				serverExitFuture.complete(null);
			});

			logger.info("Called terminate, waiting 45 seconds to close until we force-kill.");

			try {
				serverExitFuture.get(45, TimeUnit.SECONDS);
				logger.info("Eclipse-Cleanup: Process terminated gracefully.");
			} catch (TimeoutException | ExecutionException e) {
				logger.warn("Eclipse-Cleanup: Process did not terminate within 45 seconds, force-killing it.");
				try {
					process.destroyForcibly();
				} catch (Exception ex) {
					logger.error("Eclipse-Cleanup: Failed to forcibly terminate the process.", ex);
				}
			}
		}
	}

}
