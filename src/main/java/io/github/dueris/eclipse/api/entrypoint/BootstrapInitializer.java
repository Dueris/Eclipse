package io.github.dueris.eclipse.api.entrypoint;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModResource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public interface BootstrapInitializer {
	static void enter() {
		EntrypointInstance<BootstrapInitializer> entrypoint = EntrypointContainer.getEntrypoint(BootstrapInitializer.class);
		for (ModResource mod : entrypoint.getRegisteredEntrypoints()) {
			Launcher launcher = Launcher.getInstance();
			BootstrapContext context = new BootstrapContext() {
				@Override
				public @NotNull Path getModSource() {
					return mod.path().toAbsolutePath().normalize();
				}

				@Override
				public @NotNull Path getDataDirectory() {
					return ((Path) launcher.getProperties().get("modspath")).resolve(
						Objects.requireNonNull(launcher.modEngine().getContainerFromResource(mod), "Container couldnt be resolved from resource!").config().backend().getString("name")
					).toAbsolutePath().normalize();
				}

				@Override
				public @NotNull Launcher getLauncher() {
					return launcher;
				}

				@Override
				public ModContainer getModContainer() {
					return launcher.modEngine().getContainerFromResource(mod);
				}

				@Override
				public String toString() {
					return "BootstrapInitializer: " +
						getModContainer().toString() + "{,}" +
						getDataDirectory() + "{,}" +
						getModSource();
				}
			};
			entrypoint.enterSpecific(mod, context);
		}
	}

	/**
	 * Entrypoint directly before paper plugins bootstrap.
	 */
	void onInitializeBootstrap(BootstrapContext context);

	interface BootstrapContext {
		Path getModSource();

		Path getDataDirectory();

		Launcher getLauncher();

		ModContainer getModContainer();
	}
}
