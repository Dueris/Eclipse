package io.github.dueris.example;

import io.github.dueris.eclipse.loader.api.entrypoint.BootstrapInitializer;

public class EclipseBootstrapEntrypoint implements BootstrapInitializer {
	@Override
	public void onInitializeBootstrap() {
		System.out.println("TEST BOOTSTRAP ENTRYPOINT AHHHH");
	}
}
