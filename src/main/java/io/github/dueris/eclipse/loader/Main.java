package io.github.dueris.eclipse.loader;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.ember.Ember;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Main {
	public static AtomicBoolean BOOTED = new AtomicBoolean(false);
	public static Path ROOT_ABSOLUTE;

	public static void main(final String @NotNull [] arguments) {
		try {
			ROOT_ABSOLUTE = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
								.toAbsolutePath();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Invalid URI in CodeSource of Main", e);
		}

		try {
			// Prepare API and Launcher
			Launcher launcher = new EclipseLauncher();
			BOOTED.set(true);

			System.out.println("Preparing Minecraft server");
			Ember ember = new Ember();

			// Add the game.
			final Path gameJar = launcher.modEngine().gameProvider().getLaunchJar();
			try {
				System.out.println("Unpacking and linking version:" + launcher.modEngine().gameProvider().getVersion()
																			  .id() + " to " + gameJar);
				MixinJavaAgent.appendToClassPath(gameJar);

				Logger.trace("Added game jar: {}", gameJar);
			} catch (final Throwable exception) {
				Logger.error(exception, "Failed to resolve game jar: {}", gameJar);
				System.exit(1);
				return;
			}

			// Add the game libraries.
			final List<String> contained = List.of("net.sf.jopt-simple:jopt-simple:6.0-alpha-3", "net.minecrell:terminalconsoleappender:1.3.0");
			launcher.modEngine().gameProvider().getLibraries().forEach(library -> {
				if (!library.libraryPath().toString().endsWith(".jar") || contained.contains(library.libraryString()))
					return;

				try {
					String unpackMessage = "Unpacking (" + library.libraryString() + ") to " + library.libraryPath();
					if (library.trace()) {
						Logger.trace(unpackMessage);
					} else {
						System.out.println(unpackMessage);
					}
					MixinJavaAgent.appendToClassPath(library.libraryPath());

					Logger.trace("Added game library jar: {}", library);
				} catch (final Throwable exception) {
					Logger.error(exception, "Failed to resolve game library jar: {}", library);
				}
			});

			Logger.info("Loading {} {} with Eclipse version {}", launcher.modEngine().gameProvider()
																		 .getGameName(), launcher.modEngine()
																								 .gameProvider()
																								 .getVersion()
																								 .id(), IgniteConstants.IMPLEMENTATION_VERSION);

			// Launch the game.
			ember.burn(Launcher.getInstance().modEngine());
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			throw new RuntimeException("An unexpected error occurred when starting mixin server!", throwable);
		}
	}

}
