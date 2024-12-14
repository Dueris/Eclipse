package io.github.dueris.eclipse.loader.launch;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.agent.IgniteAgent;
import io.github.dueris.eclipse.loader.game.GameLibrary;
import io.github.dueris.eclipse.loader.game.GameLocatorService;
import io.github.dueris.eclipse.loader.game.GameProvider;
import io.github.dueris.eclipse.loader.util.Util;
import org.jetbrains.annotations.NotNull;

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
	public static final List<GameLibrary> LIBRARIES = new LinkedList<>();
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
		return EclipseLoaderBootstrap.instance().context.brand();
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
	public void apply(@NotNull EclipseLoaderBootstrap bootstrap) {
		// Hypothetically, paperclip doesn't even need to run, since we already have the jar executed beforehand(and is executing the eclipse jar...)
		Path gameJarPath = (Path)EmberLauncher.getProperties().get("gamejar");
		try {
			IgniteAgent.addJar(gameJarPath);
		} catch (final IOException exception) {
			throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
		}

		if (this.provider == null) {
			AtomicReference<String> game = new AtomicReference<>(""); // - Game path
			final AtomicReference<String> version = new AtomicReference<>();
			try {
				// We need this to be version-dynamic and agnostic. Do NOT preload version
				File gameJar = gameJarPath.toFile();
				if (!gameJar.exists()) {
					throw new FileNotFoundException("Game-Jar, [" + gameJarPath.toAbsolutePath() + "] was not found!");
				}
				if (gameJar.isDirectory() || !gameJar.getName().endsWith(".jar"))
					throw new IOException("Provided path is not a jar file: " + gameJar.toPath());

				try (final JarFile jarFile = new JarFile(gameJar)) {
					final JarEntry versionJEntry = jarFile.getJarEntry("version.json");
					if (versionJEntry == null) {
						throw new InsufficientResourcesException("paperclip jar didn't contain a 'version.json'! (corrupted??)");
					}

					final JarEntry versionListEntry = jarFile.getJarEntry("META-INF/versions.list");
					Util.consumePaperClipList((v) -> {
						version.set(v);
						game.set(String.format("./versions/%s", v));
					}, versionListEntry, jarFile);

					final JarEntry librariesEntry = jarFile.getJarEntry("META-INF/libraries.list");
					Util.consumePaperClipList(this::addLibrary, librariesEntry, jarFile);

					final JarEntry mainClassEntry = jarFile.getJarEntry("META-INF/main-class");
					try (final InputStream inputStream = jarFile.getInputStream(mainClassEntry);
						 final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
						targetClass = reader.readLine(); // Only 1 line, and always the 1st line
					}
				}
			} catch (Throwable throwable) {
				throw new RuntimeException("Unable to build Eclipse GameProvider!", throwable);
			}

			Util.unloadNested(LIBRARIES);
			this.provider = new EclipseGameProvider(game.get(), LIBRARIES.stream(), version.get());
		}

		EmberLauncher.getProperties().put("gamejar", this.provider.gamePath());

	}

	private void addLibrary(String libraryString, String libraryPath) {
		Path path = Paths.get("./libraries/" + libraryPath);
		LIBRARIES.add(new GameLibrary(path, libraryString, path.toFile().exists()));
	}

	@Override
	public @NotNull GameProvider locate() {
		return this.provider;
	}

	public record EclipseGameProvider(String game, Stream<GameLibrary> libraries,
									  String version) implements GameProvider {

		@Override
		public @NotNull Path gamePath() {
			return Paths.get(this.game);
		}
	}
}
