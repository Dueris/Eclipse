package io.github.dueris.eclipse.loader.minecraft;

import io.github.dueris.eclipse.loader.api.GameLibrary;
import io.github.dueris.eclipse.loader.api.McVersion;
import io.github.dueris.eclipse.loader.launch.EmberLauncher;
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

public class PaperclipJar extends JarFile {
	private final List<GameLibrary> libraries = new LinkedList<>();
	protected GameRecord gameRecord;
	private String mainClass;
	private McVersion mcVer;

	public PaperclipJar(File file) throws IOException {
		super(file);
		try {
			prepareContext();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to prepare paperclip jar", e);
		}
	}

	private void prepareContext() throws Throwable {
		AtomicReference<String> game = new AtomicReference<>("");
		AtomicReference<String> version = new AtomicReference<>();
		final JarEntry versionJEntry = this.getJarEntry("version.json");
		if (versionJEntry == null) {
			throw new InsufficientResourcesException("paperclip jar didn't contain a 'version.json'! (corrupted??)");
		}

		final JarEntry versionListEntry = this.getJarEntry("META-INF/versions.list");
		Util.consumePaperClipList((v) -> {
			version.set(v);
			game.set(String.format("./versions/%s", v));
		}, versionListEntry, this);

		final JarEntry librariesEntry = this.getJarEntry("META-INF/libraries.list");
		Util.consumePaperClipList(this::addLibrary, librariesEntry, this);

		final JarEntry mainClassEntry = this.getJarEntry("META-INF/main-class");
		try (final InputStream inputStream = this.getInputStream(mainClassEntry);
			 final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			mainClass = reader.readLine();
		}

		Util.unloadNested(libraries);
		this.gameRecord = new GameRecord(game.get(), libraries.stream(), version.get());
		mcVer = McVersionUtil.fromVersionJson(getInputStream(getJarEntry("version.json")));
		EmberLauncher.getProperties().put("gamejar", this.gameRecord.gamePath());
	}

	public String getMainClass() {
		return mainClass;
	}

	public List<GameLibrary> getLibraries() {
		return libraries;
	}

	private void addLibrary(String libraryString, String libraryPath) {
		Path path = Paths.get("./libraries/" + libraryPath);
		libraries.add(new GameLibrary(path, libraryString, path.toFile().exists()));
	}

	public McVersion mcVer() {
		return mcVer;
	}

	protected record GameRecord(String game, Stream<GameLibrary> libraries,
								String version) {

		public @NotNull Path gamePath() {
			return Paths.get(this.game);
		}
	}
}
