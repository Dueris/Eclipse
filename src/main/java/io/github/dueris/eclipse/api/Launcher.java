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
	ModEngine modEngine();
	Agent agent();
	BootstrapEntryContext entryContext();
	Map<String, Object> getProperties();
}
