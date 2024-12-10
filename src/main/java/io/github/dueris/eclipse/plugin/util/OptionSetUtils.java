package io.github.dueris.eclipse.plugin.util;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.util.PathConverter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class OptionSetUtils {

	public static final OptionParser parser = new OptionParser() {
		{
			this.acceptsAll(asList("?", "help"), "Show the help");
			this.acceptsAll(asList("c", "config"), "Properties file to use")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("server.properties"))
				.describedAs("Properties file");
			this.acceptsAll(asList("P", "plugins"), "Plugin directory to use")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("plugins"))
				.describedAs("Plugin directory");
			this.acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
				.withRequiredArg()
				.ofType(String.class)
				.describedAs("Hostname or IP");
			this.acceptsAll(asList("W", "world-dir", "universe", "world-container"), "World container")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("."))
				.describedAs("Directory containing worlds");
			this.acceptsAll(asList("w", "world", "level-name"), "World name")
				.withRequiredArg()
				.ofType(String.class)
				.describedAs("World name");
			this.acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
				.withRequiredArg()
				.ofType(Integer.class)
				.describedAs("Port");
			this.accepts("serverId", "Server ID")
				.withRequiredArg();
			this.accepts("jfrProfile", "Enable JFR profiling");
			this.accepts("pidFile", "pid File")
				.withRequiredArg()
				.withValuesConvertedBy(new PathConverter());
			this.acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
				.withRequiredArg()
				.ofType(Boolean.class)
				.describedAs("Authentication");
			this.acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
				.withRequiredArg()
				.ofType(Integer.class)
				.describedAs("Server size");
			this.acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
				.withRequiredArg()
				.ofType(SimpleDateFormat.class)
				.describedAs("Log date format");
			this.acceptsAll(asList("log-pattern"), "Specfies the log filename pattern")
				.withRequiredArg()
				.ofType(String.class)
				.defaultsTo("server.log")
				.describedAs("Log filename");
			this.acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
				.withRequiredArg()
				.ofType(Integer.class)
				.defaultsTo(0)
				.describedAs("Max log size");
			this.acceptsAll(asList("log-count"), "Specified how many log files to cycle through")
				.withRequiredArg()
				.ofType(Integer.class)
				.defaultsTo(1)
				.describedAs("Log count");
			this.acceptsAll(asList("log-append"), "Whether to append to the log file")
				.withRequiredArg()
				.ofType(Boolean.class)
				.defaultsTo(true)
				.describedAs("Log append");
			this.acceptsAll(asList("log-strip-color"), "Strips color codes from log file");
			this.acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("bukkit.yml"))
				.describedAs("Yml file");
			this.acceptsAll(asList("C", "commands-settings"), "File for command settings")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("commands.yml"))
				.describedAs("Yml file");
			this.acceptsAll(asList("forceUpgrade"), "Whether to force a world upgrade");
			this.acceptsAll(asList("eraseCache"), "Whether to force cache erase during world upgrade");
			this.acceptsAll(asList("recreateRegionFiles"), "Whether to recreate region files during world upgrade");
			this.accepts("safeMode", "Loads level with vanilla datapack only"); // Paper
			this.acceptsAll(asList("nogui"), "Disables the graphical console");
			this.acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");
			this.acceptsAll(asList("noconsole"), "Disables the console");
			this.acceptsAll(asList("v", "version"), "Show the CraftBukkit Version");
			this.acceptsAll(asList("demo"), "Demo mode");
			this.acceptsAll(asList("initSettings"), "Only create configuration files and then exit");
			this.acceptsAll(asList("S", "spigot-settings"), "File for spigot settings")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("spigot.yml"))
				.describedAs("Yml file");
			acceptsAll(asList("paper-dir", "paper-settings-directory"), "Directory for Paper settings")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File(io.papermc.paper.configuration.PaperConfigurations.CONFIG_DIR))
				.describedAs("Config directory");
			acceptsAll(asList("paper", "paper-settings"), "File for Paper settings")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File("paper.yml"))
				.describedAs("Yml file");
			acceptsAll(asList("add-plugin", "add-extra-plugin-jar"), "Specify paths to extra plugin jars to be loaded in addition to those in the plugins folder. This argument can be specified multiple times, once for each extra plugin jar path.")
				.withRequiredArg()
				.ofType(File.class)
				.defaultsTo(new File[]{})
				.describedAs("Jar file");
			acceptsAll(asList("server-name"), "Name of the server")
				.withRequiredArg()
				.ofType(String.class)
				.defaultsTo("Unknown Server")
				.describedAs("Name");
		}
	};

	private static @NotNull List<String> asList(String... params) {
		return Arrays.asList(params);
	}

	public static class Serializer {

		public static String serialize(OptionSet optionSet) throws IOException {
			return new SerializedOptionSetData().serialize(optionSet).compileString();
		}

		public static OptionSet deserialize(byte[] data) {

			SerializedOptionSetData deserializedData = new SerializedOptionSetData();
			deserializedData.decompile(data);

			return deserializedData.decompile();
		}
	}

}