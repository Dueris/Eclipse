package io.github.dueris.eclipse.api.entrypoint;

public interface ModInitializer {
	/**
	 * Main initializer for Eclipse mods. Executed immediately before `net.minecraft.server.Main.main()` is called
	 */
	void onInitialize();
}
