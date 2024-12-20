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

	/**
	 * Registers a new entrypoint instance with the given details.
	 *
	 * @param id               The unique identifier for the entrypoint.
	 * @param methodName       The name of the method to invoke in the entrypoint.
	 * @param instanceToInvoke The class of the instance to invoke the method on.
	 * @param argumentTypes    The types of the arguments for the method to invoke.
	 * @return The created and registered {@link EntrypointInstance}.
	 */
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

	/**
	 * Retrieves an entrypoint by its unique identifier.
	 *
	 * @param id The unique identifier of the entrypoint.
	 * @return The {@link EntrypointInstance} corresponding to the provided ID.
	 * @throws RuntimeException If no entrypoint with the specified ID is found.
	 */
	public static EntrypointInstance getEntrypoint(String id) {
		return entrypoints.stream().filter(i -> i.getId().equalsIgnoreCase(id)).findFirst()
						  .orElseThrow(() -> new RuntimeException("No entrypoint with that id was found!", new InstanceNotFoundException()));
	}

	/**
	 * Retrieves an entrypoint by its associated class type.
	 *
	 * @param classType The class type of the entrypoint instance.
	 * @return The {@link EntrypointInstance} corresponding to the provided class type.
	 * @throws RuntimeException If no entrypoint with the specified class type is found.
	 */
	public static EntrypointInstance getEntrypoint(Class<?> classType) {
		return entrypoints.stream().filter(i -> i.instanceClass.equals(classType)).findFirst()
						  .orElseThrow(() -> new RuntimeException("No entrypoint with that class type was found!", new InstanceNotFoundException()));
	}
}
