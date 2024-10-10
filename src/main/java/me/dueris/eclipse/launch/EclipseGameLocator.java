package me.dueris.eclipse.launch;

import me.dueris.eclipse.Util;
import org.jetbrains.annotations.NotNull;
import space.vectrix.ignite.IgniteBootstrap;
import space.vectrix.ignite.agent.IgniteAgent;
import space.vectrix.ignite.agent.transformer.PaperclipTransformer;
import space.vectrix.ignite.api.Blackboard;
import space.vectrix.ignite.game.GameLocatorService;
import space.vectrix.ignite.game.GameProvider;

import javax.naming.InsufficientResourcesException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * A more agnostic and less version-dependant game locator, based off of the Paper/Spigot GameLocator from Ignite 1.1.0
 */
public class EclipseGameLocator implements GameLocatorService {
	private static String targetClass;
	private EclipseGameProvider provider;

	public static String targetClass() {
		return targetClass;
	}

	@Override
	public @NotNull String id() {
		return "eclipse";
	}

	@Override
	public @NotNull String name() {
		return IgniteBootstrap.instance().softwareName;
	}

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public boolean shouldApply() {
		return true; // ALL paperclip jars have a 'version.json', no need to check
	}

	@Override
	public void apply(@NotNull IgniteBootstrap bootstrap) {
		// Hypothetically, paperclip doesn't even need to run, since we already have the jar executed beforehand(and is executing the eclipse jar...)
		IgniteAgent.addTransformer(new PaperclipTransformer("io/papermc/paperclip/Paperclip"));
		try {
			IgniteAgent.addJar(Blackboard.raw(Blackboard.GAME_JAR));
		} catch (final IOException exception) {
			throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
		}

		if (this.provider == null) {
			AtomicReference<String> game = new AtomicReference<>(""); // - Game path
			List<String> libraries = new LinkedList<>() {
				@Override
				public boolean add(String s) {
					return super.add("libraries\\" + s);
				}
			}; // - Game libraries
			try {
				// We need this to be version-dynamic and agnostic. Do NOT preload version
				File gameJar = Blackboard.raw(Blackboard.GAME_JAR).toFile();
				if (!gameJar.exists()) {
					throw new FileNotFoundException("Game-Jar, [" + Blackboard.raw(Blackboard.GAME_JAR).toAbsolutePath() + "] was not found!");
				}
				if (gameJar.isDirectory() || !gameJar.getName().endsWith(".jar"))
					throw new IOException("Provided path is not a jar file: " + gameJar.toPath());

				try (final JarFile jarFile = new JarFile(gameJar)) {
					final JarEntry versionJEntry = jarFile.getJarEntry("version.json");
					if (versionJEntry == null) {
						throw new InsufficientResourcesException("paperclip jar didn't contain a 'version.json'! (corrupted??)");
					}

					final JarEntry versionListEntry = jarFile.getJarEntry("META-INF/versions.list");
					Util.consumePaperClipList((v) -> game.set(String.format("./versions/%s", v)), versionListEntry, jarFile);

					final JarEntry librariesEntry = jarFile.getJarEntry("META-INF/libraries.list");
					Util.consumePaperClipList(libraries::add, librariesEntry, jarFile);

					final JarEntry mainClassEntry = jarFile.getJarEntry("META-INF/main-class");
					try (final InputStream inputStream = jarFile.getInputStream(mainClassEntry);
						 final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
						targetClass = reader.readLine(); // Only 1 line, and always the 1st line
					}
				}
			} catch (Throwable throwable) {
				throw new RuntimeException("Unable to build Eclipse GameProvider!");
			}

			this.provider = new EclipseGameProvider(game.get(), libraries);
		}

		if (Blackboard.get(Blackboard.GAME_JAR).isEmpty()) {
			Blackboard.put(Blackboard.GAME_JAR, this.provider.gamePath());
		}

	}

	@Override
	public @NotNull GameProvider locate() {
		return this.provider;
	}

	private record EclipseGameProvider(String game, List<String> libraries) implements GameProvider {
		private EclipseGameProvider(final @NotNull String game, final @NotNull List<String> libraries) {
			this.game = game;
			this.libraries = libraries;
		}

		@Override
		public @NotNull Stream<Path> gameLibraries() {
			return this.libraries.stream().map(Paths::get);
		}

		@Override
		public @NotNull Path gamePath() {
			return Paths.get(this.game);
		}
	}
}
