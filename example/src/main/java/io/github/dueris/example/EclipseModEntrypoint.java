package io.github.dueris.example;

import io.github.dueris.eclipse.api.entrypoint.ModInitializer;

public class EclipseModEntrypoint implements ModInitializer {
	@Override
	public void onInitializeServer() {
		System.out.println("TESTING!! - mod entrypoint");
	}
}
