package io.github.dueris.eclipse.api;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;

public interface Agent {
	void appendToClasspath(Path path);
	void registerClassFileTransformer(ClassFileTransformer fileTransformer);
}
