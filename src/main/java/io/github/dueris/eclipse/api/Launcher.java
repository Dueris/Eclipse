package io.github.dueris.eclipse.api;

import io.github.dueris.eclipse.api.mod.ModEngine;
import io.github.dueris.eclipse.api.util.BootstrapEntryContext;
import io.github.dueris.eclipse.loader.EclipseLauncher;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface Launcher {
	static @NotNull Launcher getInstance() {
		Launcher ret = EclipseLauncher.INSTANCE;

		if (ret == null) {
			throw new RuntimeException("Accessed EclipseLoader too early!");
		}

		return ret;
	}

	/**
	 * Retrieves the mod engine for Eclipse, which contains methods for
	 * managing and retrieving mod instances and metadata.
	 * @return The mod engine for Eclipse.
	 */
	ModEngine modEngine();

	/**
	 * Returns the API implementation of the Java Instrument API, allowing
	 * you to append files to the system classpath, and register class file transformers
	 * 
	 * @return The Eclipse JVM Agent
	 */
	Agent agent();

	/**
	 * Returns the decompiled context of the launch from the original process.
	 * 
	 * @return The {@link BootstrapEntryContext} decompiled at launch
	 */
	BootstrapEntryContext entryContext();

	/**
	 * Retrieves the properties saved and used by the Eclipse process
	 * 
	 * @return the Eclipse property data
	 */
	Map<String, Object> getProperties();

	/**
	 * Retrieves the transformer, which is in charge of managing class
	 * transformation at runtime.
	 * 
	 * @return the game transformer
	 */
	Transformer emberTransformer();
}
