package io.github.dueris.eclipse.api.entrypoint;

public interface BootstrapInitializer {
	/**
	 * Entrypoint directly before paper plugins bootstrap.
	 */
	void onInitializeBootstrap();
}
