package me.dueris.eclipse.ignite.api.util;

/**
 * Provides static access to the transformation excluded paths and packages.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class IgniteExclusions {
	/**
	 * The resource paths excluded from transformation.
	 *
	 * @since 1.0.0
	 */
	public static final String[] TRANSFORMATION_EXCLUDED_RESOURCES = {
		"org/spongepowered/asm/"
	};

	/**
	 * The packages excluded from transformation.
	 *
	 * @since 1.0.0
	 */
	public static final String[] TRANSFORMATION_EXCLUDED_PACKAGES = {
		// Launcher
		"com.astrafell.ignite.",
		"org.tinylog.",

		// Mixin
		"org.spongepowered.asm.",
		"com.llamalad7.mixinextras.",

		// Logging
		"org.slf4j.",
		"org.apache.logging.log4j.",

		// Access Widener
		"net.fabricmc.accesswidener.",
	};

	private IgniteExclusions() {
	}
}
