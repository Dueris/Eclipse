package io.github.dueris.eclipse.loader;

import io.github.dueris.eclipse.loader.agent.IgniteAgent;
import io.github.dueris.eclipse.loader.api.Blackboard;
import io.github.dueris.eclipse.loader.api.impl.MixinEngine;
import io.github.dueris.eclipse.loader.api.mod.Engine;
import io.github.dueris.eclipse.loader.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.game.GameLocatorService;
import io.github.dueris.eclipse.loader.game.GameProvider;
import io.github.dueris.eclipse.loader.launch.EclipseGameLocator;
import io.github.dueris.eclipse.loader.launch.ember.Ember;
import io.github.dueris.eclipse.loader.util.BootstrapEntryContext;
import io.github.dueris.eclipse.plugin.util.OptionSetUtils;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents the main class which starts Ignite.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class EclipseLoaderBootstrap {
	public static AtomicBoolean BOOTED = new AtomicBoolean(false);
	public static EclipseLoaderBootstrap INSTANCE;
	public static Path ROOT_ABSOLUTE;
	private final MixinEngine engine;
	public BootstrapEntryContext context;
	public GameLocatorService gameLocator;
	public String versionString;

	EclipseLoaderBootstrap() {
		EclipseLoaderBootstrap.INSTANCE = this;
		this.engine = new MixinEngine();
	}

	/**
	 * Returns the bootstrap instance.
	 *
	 * @return this instance
	 * @since 1.0.0
	 */
	public static @NotNull EclipseLoaderBootstrap instance() {
		return EclipseLoaderBootstrap.INSTANCE;
	}

	/**
	 * The main entrypoint to start Ignite.
	 *
	 * @param arguments the launch arguments
	 * @since 1.0.0
	 */
	public static void main(final String @NotNull [] arguments) {
		EclipseLoaderBootstrap ignite = new EclipseLoaderBootstrap();
		try {
			ROOT_ABSOLUTE = Path.of(EclipseLoaderBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Invalid URI in CodeSource of EclipseLoaderBootstrap", e);
		}

		ignite.context = BootstrapEntryContext.read();
		Path jarPath = ignite.context.serverPath();

		Blackboard.GAME_JAR = Blackboard.key("ignite.jar", Path.class, jarPath);
		Blackboard.compute(Blackboard.GAME_JAR, () -> jarPath);
		Blackboard.compute(Blackboard.DEBUG, () -> Boolean.parseBoolean(System.getProperty(Blackboard.DEBUG.name())));
		Blackboard.compute(Blackboard.GAME_LIBRARIES, () -> Paths.get(System.getProperty(Blackboard.GAME_LIBRARIES.name())));
		Blackboard.compute(Blackboard.MODS_DIRECTORY, () -> Paths.get(System.getProperty(Blackboard.MODS_DIRECTORY.name())));

		BOOTED.set(true);
		ignite.ignite();
	}

	/**
	 * Returns the {@link Engine}.
	 *
	 * @return the mods
	 * @since 1.0.0
	 */
	public static @NotNull Engine mods() {
		return instance().engine;
	}

	private void ignite() {
		// Get a suitable game locator and game provider.
		System.out.println("Preparing Minecraft server");
		final GameProvider gameProvider;
		{
			gameLocator = new EclipseGameLocator();

			try {
				gameLocator.apply(this);
			} catch (final Throwable throwable) {
				Logger.error(throwable, "Failed to start game: Unable to apply GameLocator service.");
				System.exit(1);
				return;
			}

			gameProvider = gameLocator.locate();
		}

		// Add the game.
		final Path gameJar = gameProvider.gamePath();
		versionString = ((EclipseGameLocator.EclipseGameProvider) EclipseLoaderBootstrap.instance().gameLocator.locate()).version().split("/")[0];
		try {
			System.out.println("Unpacking and linking version:" + versionString + " to " + gameJar);
			IgniteAgent.addJar(gameJar);

			Logger.trace("Added game jar: {}", gameJar);
		} catch (final IOException exception) {
			Logger.error(exception, "Failed to resolve game jar: {}", gameJar);
			System.exit(1);
			return;
		}

		// Add the game libraries.
		final List<String> contained = List.of("net.sf.jopt-simple:jopt-simple:6.0-alpha-3", "net.minecrell:terminalconsoleappender:1.3.0");
		gameProvider.libraries().forEach(library -> {
			if (!library.libraryPath().toString().endsWith(".jar") || contained.contains(library.libraryString()))
				return;

			try {
				if (library.trace()) {
					Logger.trace("Unpacking (" + library.libraryString() + ") to " + library.libraryPath());
				} else {
					System.out.println("Unpacking (" + library.libraryString() + ") to " + library.libraryPath());
				}
				IgniteAgent.addJar(library.libraryPath());

				Logger.trace("Added game library jar: {}", library);
			} catch (final IOException exception) {
				Logger.error(exception, "Failed to resolve game library jar: {}", library);
			}
		});

		Logger.info("Loading Minecraft {} with Eclipse version {}", versionString, IgniteConstants.IMPLEMENTATION_VERSION);

		// Launch the game.
		Ember.launch(OptionSetUtils.Serializer.deserialize(context.optionSet()));
	}

	/**
	 * Returns the mod engine.
	 *
	 * @return the mod engine
	 * @since 1.0.0
	 */
	public @NotNull MixinEngine engine() {
		return this.engine;
	}
}
