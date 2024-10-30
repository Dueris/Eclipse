package me.dueris.eclipse;

import com.dragoncommissions.mixbukkit.api.shellcode.impl.api.CallbackInfo;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.nio.file.Path;

public class Injectors {

	public static void eclipseLoadWrapper(Path path, OptionSet optionset, CallbackInfo info) {
		BootstrapEntrypoint.logger.info("Starting Eclipse/Ignite bootstrap...");
		BootstrapEntrypoint entrypoint = new BootstrapEntrypoint();
		try {
			entrypoint.prepLaunch();
			entrypoint.executePlugin(
				BootstrapEntrypoint.CONTEXT.getPluginSource().toFile(), optionset
			);
		} catch (Exception ea) {
			throw new RuntimeException(ea);
		}
	}
}
