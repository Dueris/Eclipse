package io.github.dueris.eclipse.api.util;

public final class IgniteExclusions {

	public static final String[] TRANSFORMATION_EXCLUDED_RESOURCES = {
		"org/spongepowered/asm/"
	};

	public static final String[] TRANSFORMATION_EXCLUDED_PACKAGES = {
		// Launcher
		"io.github.dueris.eclipse.",
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
