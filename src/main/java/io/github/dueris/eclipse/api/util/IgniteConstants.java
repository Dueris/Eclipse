package io.github.dueris.eclipse.api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.dueris.eclipse.loader.Main;
import org.objectweb.asm.Opcodes;

public final class IgniteConstants {
	public static final String API_TITLE = Main.class.getPackage().getSpecificationTitle();

	public static final String API_VERSION = Main.class.getPackage().getSpecificationVersion();

	public static final String IMPLEMENTATION_VERSION = Main.class.getPackage().getImplementationVersion();

	public static final int ASM_VERSION = Opcodes.ASM9;

	public static final String MOD_CONFIG_YML = "paper-plugin.yml";

	public static final String MOD_CACHE_DIR = "META-INF/mods/";

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private IgniteConstants() {
	}
}
