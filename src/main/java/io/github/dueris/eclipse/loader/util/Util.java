package io.github.dueris.eclipse.loader.util;

import io.github.dueris.eclipse.api.GameLibrary;
import io.github.dueris.eclipse.loader.Main;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collector;

public class Util {
	static final Set<Collector.Characteristics> CH_ID
		= Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

	public static void consumePaperClipList(Consumer<String> lineConsumer, JarEntry entry, JarFile jarFile) throws Throwable {
		try (final InputStream inputStream = jarFile.getInputStream(entry); final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] values = line.split("\t");

				if (values.length >= 3) {
					lineConsumer.accept(values[2]);
				}
			}
		}
	}

	public static void consumePaperClipList(BiConsumer<String, String> lineConsumer, JarEntry entry, JarFile jarFile) throws Throwable {
		try (final InputStream inputStream = jarFile.getInputStream(entry); final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] values = line.split("\t");

				if (values.length >= 3) {
					lineConsumer.accept(values[1], values[2]);
				}
			}
		}
	}

	public static <T> @NotNull Collector<T, ?, LinkedHashSet<T>> toLinkedSet() {
		return new CollectorImpl<>(LinkedHashSet::new, Set::add,
			(left, right) -> {
				if (left.size() < right.size()) {
					right.addAll(left);
					return right;
				} else {
					left.addAll(right);
					return left;
				}
			}, CH_ID);
	}

	@SuppressWarnings("unchecked")
	private static <I, R> @NotNull Function<I, R> castingIdentity() {
		return i -> (R) i;
	}

	public static void unloadNested(List<GameLibrary> libraries) {
		try {
			@SuppressWarnings("resource") JarFile jarFile = new JarFile(Main.ROOT_ABSOLUTE.toFile());
			String entryName = "/nested-libs/console-v1.0.0.jar";
			Path root = Paths.get(".");
			Path outputDirPath = root.toAbsolutePath().resolve("cache").resolve(".eclipse").resolve("server");
			AtomicReference<JarEntry> entry = new AtomicReference<>();
			jarFile.entries().asIterator().forEachRemaining(e -> {
				if (e.getName().contains("console-v1.0.0.jar")) {
					entry.set(e);
				}
			});
			if (entry.get() == null) {
				throw new IOException("Entry '" + entryName + "' not found in JAR: " + jarFile.getName());
			}

			if (!Files.exists(outputDirPath)) {
				Files.createDirectories(outputDirPath);
			}

			Path outputFilePath = outputDirPath.resolve(new File(entryName).getName());
			boolean existsAlready = outputFilePath.toFile().exists();

			try (InputStream inputStream = jarFile.getInputStream(entry.get());
				 FileOutputStream outputStream = new FileOutputStream(outputFilePath.toFile())) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
			}

			// Download required libraries and add them
			libraries.add(new GameLibrary(
				outputFilePath.toAbsolutePath()
					.normalize(), "patched//net.minecrell:terminalconsoleappender:1.3.0", existsAlready
			));
			libraries.add(downloadLibrary("net.fabricmc:access-widener:2.1.0", "https://maven.fabricmc.net/", root.resolve("libraries")
				.toAbsolutePath()
				.normalize()
				.toString()));
			libraries.add(downloadLibrary("io.github.llamalad7:mixinextras-common:0.4.1", "https://repo.maven.apache.org/maven2/", root.resolve("libraries")
				.toAbsolutePath()
				.normalize()
				.toString()));
		} catch (IOException e) {
			throw new RuntimeException("Unable to unload nested jars!", e);
		}
	}

	public static @NotNull GameLibrary downloadLibrary(@NotNull String library, String repositoryUrl, String outputDir) throws IOException {
		String[] parts = library.split(":");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid library format. Use group:artifact:version");
		}

		String groupId = parts[0];
		String artifactId = parts[1];
		String version = parts[2];

		String groupPath = groupId.replace(".", "/");
		String jarFileName = artifactId + "-" + version + ".jar";
		String jarPath = groupPath + "/" + artifactId + "/" + version + "/" + jarFileName;

		Pair<File, Boolean> out = download(repositoryUrl, outputDir, jarPath);

		Logger.trace("Downloaded: " + out.first().getAbsolutePath());
		return new GameLibrary(out.first().toPath().toAbsolutePath().normalize(), library, out.second());
	}

	private static @NotNull Pair<File, Boolean> download(@NotNull String repositoryUrl, String outputDir, String jarPath) throws IOException {
		String jarUrl = repositoryUrl.endsWith("/") ? repositoryUrl + jarPath : repositoryUrl + "/" + jarPath;
		File outputFile = new File(outputDir, jarPath);

		if (!outputFile.getParentFile().exists()) {
			if (!outputFile.getParentFile().mkdirs()) {
				throw new IOException("Failed to create directories for: " + outputFile.getParent());
			}
		}

		boolean trace = outputFile.exists();
		try (BufferedInputStream in = new BufferedInputStream(new URL(jarUrl).openStream());
			 FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
		}
		return new Pair<>(outputFile, trace);
	}

	record CollectorImpl<T, A, R>(Supplier<A> supplier,
								  BiConsumer<A, T> accumulator,
								  BinaryOperator<A> combiner,
								  Function<A, R> finisher,
								  Set<Characteristics> characteristics
	) implements Collector<T, A, R> {

		CollectorImpl(Supplier<A> supplier,
					  BiConsumer<A, T> accumulator,
					  BinaryOperator<A> combiner,
					  Set<Characteristics> characteristics) {
			this(supplier, accumulator, combiner, castingIdentity(), characteristics);
		}
	}
}
