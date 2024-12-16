package com.dragoncommissions.mixbukkit;

import com.dragoncommissions.mixbukkit.agent.ClassesManager;
import com.dragoncommissions.mixbukkit.agent.JVMAttacher;
import com.dragoncommissions.mixbukkit.api.MixinPlugin;
import com.dragoncommissions.mixbukkit.api.ObfMap;
import com.dragoncommissions.mixbukkit.utils.io.BukkitErrorOutputStream;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

public class MixBukkit {

	public final static String VERSION = "0.1";
	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MixBukkit.class);

	@Getter
	private static final Map<String, MixinPlugin> plugins = new HashMap<>();
	public static MixBukkit INSTANCE;
	public static boolean DEBUG = true;
	public static boolean WRITE_TRANSFORMED_CLASS = true;
	public static boolean SAFE_MODE = false;
	public static Instrumentation INSTRUMENTATION = null;
	public static boolean PREPARED = false;
	public static BukkitErrorOutputStream ERROR_OUTPUT_STREAM = new BukkitErrorOutputStream();
	public static ClassesManager classesManager;

	@ApiStatus.Internal
	public static AtomicReference<PaperPluginClassLoader> CLASSLOADER = new AtomicReference<>();

	@Getter
	private static JVMAttacher jvmAttacher;

	@Getter
	private File pluginFile;

	public MixBukkit(PaperPluginClassLoader classLoader) {
		CLASSLOADER.set(classLoader);
	}

	public static PaperPluginClassLoader getClassLoader() {
		return CLASSLOADER.get();
	}

	@SneakyThrows
	public static void addLibrary(File file) {
		try {
			INSTRUMENTATION.appendToSystemClassLoaderSearch(new JarFile(file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (DEBUG) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "// Loading " + file.getAbsolutePath());
		}
	}

	@SneakyThrows
	public void onEnable(Logger logger, File pluginFile) {
		this.pluginFile = pluginFile;
		INSTANCE = this;

		jvmAttacher = new JVMAttacher(this);
		jvmAttacher.attach();
		if (INSTRUMENTATION == null) {
			try {
				logger.warn("Failed grabbing instrumentation! If you believe this is an issue, please open a ticket");
				logger.warn("");
				logger.warn("======= FAILED GETTING INSTRUMENTATION ======");
				logger.warn("Please check those things before opening an issue:");
				logger.warn("1. Do you have -XX:+DisableAttachMechanism? If yes, remove it from server start command.");
				logger.warn("2. Does the server have permission to spawn a process? If no, give it. Normally yes unless you are using a server panel that limits the privilege");
				logger.warn("");
				logger.warn("=============================================");

				throw new NullPointerException("Instrumentation is null");
			} catch (Exception e) {
				logger.error("An error occurred:", e);
			}
		}

		ClassesManager.init();
		PREPARED = true;
	}

	public @NotNull MixinPlugin registerMixinPlugin(@NotNull MixinPluginInstance plugin, InputStream membersMapStream) {
		MixinPlugin mixinPlugin = plugins.get(plugin.name());
		if (mixinPlugin != null) {
			return mixinPlugin;
		}
		mixinPlugin = new MixinPlugin(plugin, new ObfMap(membersMapStream));
		plugins.put(plugin.name(), mixinPlugin);


		return mixinPlugin;
	}

}
