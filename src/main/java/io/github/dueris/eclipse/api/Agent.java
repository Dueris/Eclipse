package io.github.dueris.eclipse.api;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;

public interface Agent {
	/**
	 * Adds the given {@link Path} to the system classpath
	 * @param path the {@link Path} to register
	 */
	void appendToClasspath(Path path);

	/**
	 * Registers a new {@link ClassFileTransformer} to the system classpath
	 * @param fileTransformer the {@link ClassFileTransformer} to register
	 */
	void registerClassFileTransformer(ClassFileTransformer fileTransformer);
}
