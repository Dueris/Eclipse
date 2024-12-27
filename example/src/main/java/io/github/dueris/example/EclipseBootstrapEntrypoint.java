package io.github.dueris.example;

import io.github.dueris.eclipse.api.entrypoint.BootstrapInitializer;

public class EclipseBootstrapEntrypoint implements BootstrapInitializer {
	@Override
	public void onInitializeBootstrap(BootstrapContext context) {
		System.out.println(context);
		System.out.println("TEST BOOTSTRAP ENTRYPOINT AHHHH");
	}
}
