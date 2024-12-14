package io.github.dueris.eclipse.loader.entrypoint;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.api.impl.ModMetadata;
import io.github.dueris.eclipse.loader.api.mod.ModContainer;
import io.github.dueris.eclipse.loader.api.mod.ModResource;

import javax.management.InstanceNotFoundException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntrypointContainer {
	private static final List<EntrypointInstance> entrypoints = new CopyOnWriteArrayList<>();

	/**
	 * Registers your entrypoint and prepares it for calling
	 * @param id the id that will be associated with your mod entrypoint
	 * @param methodName the name of the method to invoke, like `onInitialize()`
	 * @param instanceToInvoke the class type to invoke(must be an interface)
	 */
	public static EntrypointInstance register(String id, String methodName, Class<?> instanceToInvoke, Class<?>... argumentTypes) {
		EntrypointInstance instance = new EntrypointInstance<>(id, methodName, instanceToInvoke, argumentTypes);
		entrypoints.add(instance);
		for (ModContainer modContainer : EclipseLoaderBootstrap.instance().engine().containers()) {
			ModMetadata metadata = modContainer.config();
			ModResource resource = modContainer.resource();
			instance.prepare();
			instance.buildEntrypoints(metadata.backend().contains("entrypoints") ? metadata.backend().getConfigurationSection("entrypoints") : null, resource);
		}
		return instance;
	}

	public static EntrypointInstance getEntrypoint(String id) {
		return entrypoints.stream().filter(i -> i.getId().equalsIgnoreCase(id)).findFirst().orElseThrow(() -> new RuntimeException("No entrypoint with that id was found!", new InstanceNotFoundException()));
	}

	public static EntrypointInstance getEntrypoint(Class<?> classType) {
		return entrypoints.stream().filter(i -> i.instanceClass.equals(classType)).findFirst().orElseThrow(() -> new RuntimeException("No entrypoint with that class type was found!", new InstanceNotFoundException()));
	}
}
