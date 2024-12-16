package io.github.dueris.eclipse.loader;

import io.github.dueris.eclipse.api.Agent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class MixinJavaAgent implements Agent {
	static Agent INSTANCE;
	private static Instrumentation INSTRUMENTATION = null;

	private MixinJavaAgent() {
		INSTANCE = this;
	}

	public static void appendToClassPath(final @NotNull Path path) throws IOException {
		final File file = path.toFile();
		if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
		if (file.isDirectory() || !file.getName().endsWith(".jar"))
			throw new IOException("Provided path is not a jar file: " + path);
		MixinJavaAgent.appendToClassPath(new JarFile(file));
	}

	public static void appendToClassPath(final @NotNull JarFile jar) {
		if (MixinJavaAgent.INSTRUMENTATION != null) {
			MixinJavaAgent.INSTRUMENTATION.appendToSystemClassLoaderSearch(jar);
			return;
		}

		throw new IllegalStateException("Unable to addJar for '" + jar.getName() + "'.");
	}

	public static void premain(final @NotNull String arguments, final @Nullable Instrumentation instrumentation) {
		MixinJavaAgent.agentmain(arguments, instrumentation);
	}

	public static void agentmain(final @NotNull String arguments, final @Nullable Instrumentation instrumentation) {
		if (MixinJavaAgent.INSTRUMENTATION == null) MixinJavaAgent.INSTRUMENTATION = instrumentation;
		if (MixinJavaAgent.INSTRUMENTATION == null)
			throw new NullPointerException("Unable to get instrumentation instance!");
	}

	@Override
	public void appendToClasspath(Path path) {
		try {
			MixinJavaAgent.appendToClassPath(path);
		} catch (IOException e) {
			throw new RuntimeException("Unable to append to classpath!", e);
		}
	}

	@Override
	public void registerTransformer(ClassFileTransformer fileTransformer) {
		INSTRUMENTATION.addTransformer(fileTransformer);
	}
}
