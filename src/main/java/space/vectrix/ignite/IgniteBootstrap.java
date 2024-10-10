package space.vectrix.ignite;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.dueris.eclipse.launch.EclipseGameLocator;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.asm.util.asm.ASM;
import org.tinylog.Logger;
import space.vectrix.ignite.agent.IgniteAgent;
import space.vectrix.ignite.api.Blackboard;
import space.vectrix.ignite.api.Platform;
import space.vectrix.ignite.api.mod.Mods;
import space.vectrix.ignite.api.mod.ModsImpl;
import space.vectrix.ignite.api.util.IgniteConstants;
import space.vectrix.ignite.game.GameLocatorService;
import space.vectrix.ignite.game.GameProvider;
import space.vectrix.ignite.launch.ember.Ember;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents the main class which starts Ignite.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class IgniteBootstrap {
	public static AtomicBoolean BOOTED = new AtomicBoolean(false);
	private static IgniteBootstrap INSTANCE;
	private static Platform PLATFORM;
	private final ModsImpl engine;
	public String softwareName;

	IgniteBootstrap() {
		IgniteBootstrap.INSTANCE = this;
		this.engine = new ModsImpl();
	}

	/**
	 * Returns the bootstrap instance.
	 *
	 * @return this instance
	 * @since 1.0.0
	 */
	public static @NotNull IgniteBootstrap instance() {
		return IgniteBootstrap.INSTANCE;
	}

	/**
	 * The main entrypoint to start Ignite.
	 *
	 * @param arguments the launch arguments
	 * @since 1.0.0
	 */
	public static void main(final String @NotNull [] arguments) {
		String asciiArt = """
			--------------------------------------------------------------------------
			 _____ ____ _     ___ ____  ____  _____      __  __ _____  _____ _   _\s
			| ____/ ___| |   |_ _|  _ \\/ ___|| ____|    |  \\/  |_ _\\ \\/ /_ _| \\ | |
			|  _|| |   | |    | || |_) \\___ \\|  _| _____| |\\/| || | \\  / | ||  \\| |
			| |__| |___| |___ | ||  __/ ___) | |__|_____| |  | || | /  \\ | || |\\  |
			|_____\\____|_____|___|_|   |____/|_____|    |_|  |_|___/_/\\_\\___|_| \\_|
			--------------------------------------------------------------------------
			""";

		Logger.info("\n{}" + "Running {} v{} (API: {}, ASM: {}, Java: {})", asciiArt,
			IgniteConstants.API_TITLE,
			IgniteConstants.IMPLEMENTATION_VERSION,
			IgniteConstants.API_VERSION,
			ASM.getVersionString(),
			JavaVersion.current());

		JsonObject bootstrapInfo = new Gson().fromJson(((Getter<String>) () -> {
			File bootstrapFile = Paths.get("eclipse.mixin.bootstrap.json").toFile();
			if (!bootstrapFile.exists()) {
				throw new IllegalStateException("Unable to find bootstrap json! Did Eclipse start correctly?");
			}

			try {
				return Files.readString(bootstrapFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException("Unable to build String contents of Bootstrap!", e);
			}
		}).get(), JsonObject.class);


		String serverPath = bootstrapInfo.get("ServerPath").getAsString();
		if (serverPath.startsWith("/")) {
			serverPath = serverPath.substring(1);
		}
		Path jarPath = Path.of(serverPath);

		Blackboard.GAME_JAR = Blackboard.key("ignite.jar", Path.class, jarPath);
		Blackboard.compute(Blackboard.GAME_JAR, () -> jarPath);
		Blackboard.compute(Blackboard.DEBUG, () -> Boolean.parseBoolean(System.getProperty(Blackboard.DEBUG.name())));
		Blackboard.compute(Blackboard.GAME_LIBRARIES, () -> Paths.get(System.getProperty(Blackboard.GAME_LIBRARIES.name())));
		Blackboard.compute(Blackboard.MODS_DIRECTORY, () -> Paths.get(System.getProperty(Blackboard.MODS_DIRECTORY.name())));

		IgniteBootstrap ignite = new IgniteBootstrap();
		ignite.softwareName = bootstrapInfo.get("SoftwareName").getAsString();
		BOOTED.set(true);

		ignite.run(arguments);
	}

	static void initialize(final @NotNull Platform platform) {
		IgniteBootstrap.PLATFORM = platform;
	}

	/**
	 * Returns the {@link Mods}.
	 *
	 * @return the mods
	 * @since 1.0.0
	 */
	public static @NotNull Mods mods() {
		if (IgniteBootstrap.PLATFORM == null) throw new IllegalStateException("Ignite has not been initialized yet!");
		return IgniteBootstrap.PLATFORM.mods();
	}

	private void run(final String @NotNull [] args) {
		final List<String> arguments = Arrays.asList(args);
		final List<String> launchArguments = new ArrayList<>(arguments);

		// move Blackboard building to main()

		// Get a suitable game locator and game provider.
		final GameLocatorService gameLocator;
		final GameProvider gameProvider;
		{
			final Optional<EclipseGameLocator> gameLocatorProvider = Optional.of(new EclipseGameLocator());

			gameLocator = gameLocatorProvider.get();

			Logger.info("Detected game locator: {}", gameLocator.name());

			try {
				gameLocator.apply(this);
			} catch (final Throwable throwable) {
				Logger.error(throwable, "Failed to start game: Unable to apply GameLocator service.");
				System.exit(1);
				return;
			}

			gameProvider = gameLocator.locate();
		}

		Logger.info("Preparing the game...");

		// Add the game.
		final Path gameJar = gameProvider.gamePath();
		try {
			IgniteAgent.addJar(gameJar);

			Logger.trace("Added game jar: {}", gameJar);
		} catch (final IOException exception) {
			Logger.error(exception, "Failed to resolve game jar: {}", gameJar);
			System.exit(1);
			return;
		}

		// Add the game libraries.
		IgniteAgent.log = false;
		gameProvider.gameLibraries().forEach(path -> {
			if (!path.toString().endsWith(".jar")) return;

			try {
				IgniteAgent.addJar(path);

				Logger.trace("Added game library jar: {}", path);
			} catch (final IOException exception) {
				Logger.error(exception, "Failed to resolve game library jar: {}", path);
			}
		});
		Logger.info("Loaded {} game libraries into the ignite classpath", gameProvider.gameLibraries().count());
		IgniteAgent.log = true;

		// Initialize the API.
		IgniteBootstrap.initialize(new PlatformImpl());

		Logger.info("Launching ember...");

		// Launch the game.
		Ember.launch(launchArguments.toArray(new String[0]));
	}

	/**
	 * Returns the mod engine.
	 *
	 * @return the mod engine
	 * @since 1.0.0
	 */
	public @NotNull ModsImpl engine() {
		return this.engine;
	}

	private interface Getter<T> {
		T get();
	}
}
