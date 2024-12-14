package io.github.dueris.eclipse.loader.agent;

import io.github.dueris.eclipse.loader.agent.patch.GamePatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * Provides static access to add additional resources to the system
 * classloader.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class IgniteAgent {
	private static Instrumentation INSTRUMENTATION = null;

	private IgniteAgent() {
	}

	public static <T extends GamePatch> void addPatch(final @NotNull T patch) {
		if (IgniteAgent.INSTRUMENTATION != null) IgniteAgent.INSTRUMENTATION.addTransformer(patch);
	}

	public static <T extends GamePatch> void addPatch(@NotNull Class<T> patchClass) {
		try {
			addPatch(patchClass.getConstructor().newInstance());
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
				 NoSuchMethodException e) {
			throw new RuntimeException("Unable to add patch to classpath agent!", e);
		}
	}

	/**
	 * Adds a jar {@link Path} to this agent.
	 *
	 * @param path the path
	 * @throws IOException if there is an error resolving the path
	 * @since 1.0.0
	 */
	public static void addJar(final @NotNull Path path) throws IOException {
		final File file = path.toFile();
		if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
		if (file.isDirectory() || !file.getName().endsWith(".jar"))
			throw new IOException("Provided path is not a jar file: " + path);
		IgniteAgent.addJar(new JarFile(file));
	}

	/**
	 * Adds a {@link JarFile} to this agent.
	 *
	 * @param jar the jar file
	 * @since 1.0.0
	 */
	public static void addJar(final @NotNull JarFile jar) {
		if (IgniteAgent.INSTRUMENTATION != null) {
			IgniteAgent.INSTRUMENTATION.appendToSystemClassLoaderSearch(jar);
			return;
		}

		throw new IllegalStateException("Unable to addJar for '" + jar.getName() + "'.");
	}

	/**
	 * The agent premain entrypoint.
	 *
	 * @param arguments       the arguments
	 * @param instrumentation the instrumentation
	 * @since 1.0.0
	 */
	public static void premain(final @NotNull String arguments, final @Nullable Instrumentation instrumentation) {
		IgniteAgent.agentmain(arguments, instrumentation);
	}

	/**
	 * The agent main entrypoint.
	 *
	 * @param arguments       the arguments
	 * @param instrumentation the instrumentation
	 * @since 1.0.0
	 */
	public static void agentmain(final @NotNull String arguments, final @Nullable Instrumentation instrumentation) {
		if (IgniteAgent.INSTRUMENTATION == null) IgniteAgent.INSTRUMENTATION = instrumentation;
		if (IgniteAgent.INSTRUMENTATION == null)
			throw new NullPointerException("Unable to get instrumentation instance!");
	}
}
