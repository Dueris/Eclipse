package io.github.dueris.eclipse.plugin.util;

import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.CallbackInfo;
import joptsimple.OptionSet;

import java.nio.file.Path;

public class Injectors {

	public static void eclipseLoadWrapper(Path path, OptionSet optionset, CallbackInfo info) {
		BootstrapEntrypoint.logger.info("Starting Eclipse bootstrap...");
		BootstrapEntrypoint entrypoint = new BootstrapEntrypoint();
		try {
			entrypoint.prepLaunch();
			entrypoint.executePlugin(
				BootstrapEntrypoint.CONTEXT.getPluginSource().toFile(), optionset
			);
		} catch (Exception ea) {
			throw new RuntimeException("Unable to launch eclipse server!", ea);
		}
	}
}
