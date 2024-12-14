package io.github.dueris.eclipse.plugin.mixin;

import io.github.dueris.eclipse.loader.api.entrypoint.ModInitializer;
import io.github.dueris.eclipse.loader.entrypoint.EntrypointContainer;
import io.github.dueris.eclipse.plugin.access.EclipseMain;
import joptsimple.OptionSet;
import net.minecraft.SharedConstants;
import org.bukkit.craftbukkit.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

@Mixin(value = Main.class, priority = 1)
public class MainMixin implements EclipseMain {

	@Shadow
	public static boolean useJline;

	@Shadow
	public static boolean useConsole;

	@Override
	public void eclipse$main(OptionSet options) {
		String path = new File(".").getAbsolutePath();
		if (path.contains("!") || path.contains("+")) {
			System.err.println("Cannot run server in a directory with ! or + in the pathname. Please rename the affected folders and try again.");
			return;
		}

		try {
			if (options.has("nojline")) {
				System.setProperty(net.minecrell.terminalconsole.TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false");
				useJline = false;
			}

			if (options.has("noconsole")) {
				Main.useConsole = false;
				useJline = false;
				System.setProperty(net.minecrell.terminalconsole.TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false"); // Paper
			}

			System.setProperty("library.jansi.version", "Paper");
			System.setProperty("jdk.console", "java.base");

			SharedConstants.tryDetectVersion();

			EntrypointContainer.getEntrypoint(ModInitializer.class).enter();
			net.minecraft.server.Main.main(options);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
