package io.github.dueris.eclipse.loader.launch.patch;

import org.tinylog.Logger;

public class PatchHooks {
	public static final String INTERNAL_NAME = PatchHooks.class.getName().replace('.', '/');
	public static final String ECLIPSE = "eclipse";
	public static final String VANILLA = "Paper";

	public static String insertBranding(final String brand) {
		if (brand == null || brand.isEmpty()) {
			Logger.warn("Null or empty branding found!", new IllegalStateException());
			return ECLIPSE;
		}

		return VANILLA.equals(brand) ? ECLIPSE : brand + "/(" + ECLIPSE + ")";
	}
}
