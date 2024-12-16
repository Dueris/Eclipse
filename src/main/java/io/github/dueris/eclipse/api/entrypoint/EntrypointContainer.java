package io.github.dueris.eclipse.api.entrypoint;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModMetadata;
import io.github.dueris.eclipse.api.mod.ModResource;
import org.jetbrains.annotations.NotNull;

import javax.management.InstanceNotFoundException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntrypointContainer {
	private static final List<EntrypointInstance> entrypoints = new CopyOnWriteArrayList<>();

	public static @NotNull EntrypointInstance register(String id, String methodName, Class<?> instanceToInvoke, Class<?>... argumentTypes) {
		EntrypointInstance instance = new EntrypointInstance<>(id, methodName, instanceToInvoke, argumentTypes);
		entrypoints.add(instance);
		for (ModContainer modContainer : Launcher.getInstance().modEngine().containers()) {
			ModMetadata metadata = modContainer.config();
			ModResource resource = modContainer.resource();
			instance.prepare();
			instance.buildEntrypoints(metadata.backend().contains("entrypoints") ? metadata.backend()
																						   .getConfigurationSection("entrypoints") : null, resource);
		}
		return instance;
	}

	public static EntrypointInstance getEntrypoint(String id) {
		return entrypoints.stream().filter(i -> i.getId().equalsIgnoreCase(id)).findFirst()
						  .orElseThrow(() -> new RuntimeException("No entrypoint with that id was found!", new InstanceNotFoundException()));
	}

	public static EntrypointInstance getEntrypoint(Class<?> classType) {
		return entrypoints.stream().filter(i -> i.instanceClass.equals(classType)).findFirst()
						  .orElseThrow(() -> new RuntimeException("No entrypoint with that class type was found!", new InstanceNotFoundException()));
	}
}
