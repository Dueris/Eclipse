package io.github.dueris.eclipse.api.entrypoint;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.mod.ModResource;
import io.github.dueris.eclipse.loader.ember.EmberClassLoader;
import org.jetbrains.annotations.*;
import org.simpleyaml.configuration.ConfigurationSection;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EntrypointInstance<T> {
	protected final Class<T> instanceClass;
	private final String id;
	private final String methodName;
	private final Class<?>[] argumentTypes;
	private final Map<ModResource, Class<? extends T>> registeredEntrypoints = new HashMap<>();

	EntrypointInstance(String id, String methodName, Class<T> interfaceClass, Class<?>... argumentTypes) {
		this.id = id;
		this.methodName = methodName;
		this.instanceClass = verify(interfaceClass);
		this.argumentTypes = argumentTypes;
	}

	/**
	 * Verifies that the provided class is valid.
	 *
	 * @param toVerify The class to verify.
	 * @return The verified class.
	 * @throws RuntimeException If the class is invalid.
	 */
	@Contract("_ -> param1")
	private @NotNull Class<T> verify(@NotNull Class<T> toVerify) {
		if (!toVerify.isInterface()) {
			throw new RuntimeException("Class instance is not an interface! " + toVerify.getName(), new IllegalClassFormatException());
		}
		return toVerify;
	}

	/**
	 * Enters the entrypoint for each registered mod resource by invoking the method
	 * with the provided arguments.
	 *
	 * @param arguments The arguments to pass to the entrypoint method.
	 * @throws RuntimeException If any error occurs during method invocation.
	 */
	public void enter(Object... arguments) {
		for (ModResource mod : this.registeredEntrypoints.keySet()) {
			enterSpecific(mod, arguments);
		}
	}

	@ApiStatus.Internal
	void enterSpecific(@NotNull final ModResource mod, Object... arguments) {
		Class<? extends T> entrypointClass = this.registeredEntrypoints.get(mod);
		try {
			Method method = entrypointClass.getDeclaredMethod(methodName, argumentTypes);
			method.setAccessible(true);
			T entrypointInstance = entrypointClass.getConstructor().newInstance();
			method.invoke(entrypointInstance, arguments);
		} catch (Throwable throwable) {
			throw new RuntimeException("Unable to enter mod, " + Launcher.getInstance().modEngine()
				.getContainerFromResource(mod) + " !", throwable);
		}
	}

	/**
	 * Retrieves the unique identifier for this entrypoint.
	 *
	 * @return The unique ID of this entrypoint.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Builds the entrypoints from the provided configuration section and mod resource.
	 * It associates the entrypoints with their corresponding mod resources.
	 *
	 * @param entrypointContainer The configuration section containing the entrypoint definitions.
	 * @param resource            The mod resource associated with the entrypoint.
	 * @throws RuntimeException If any error occurs while resolving or validating entrypoints.
	 */
	@SuppressWarnings("unchecked")
	void buildEntrypoints(@Nullable ConfigurationSection entrypointContainer, @NotNull ModResource resource) {
		if (entrypointContainer != null) {
			if (entrypointContainer.contains(id)) {
				String entryName = entrypointContainer.getString(id);
				try {
					Class<?> clazz = Class.forName(entryName, true, EmberClassLoader.INSTANCE);
					if (instanceClass.isAssignableFrom(clazz)) {
						registeredEntrypoints.put(resource, (Class<? extends T>) clazz);
					} else {
						throw new RuntimeException("Class in mod entrypoint container is not an implementation of the required interface!");
					}
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Unable to find class: " + entryName, e);
				}
			}
		}
	}

	/**
	 * Prepares the entrypoint for building
	 */
	void prepare() {
		registeredEntrypoints.clear();
	}

	/**
	 * Returns the registered entrypoints of the entrypoint. Unmodifiable
	 */
	public @Unmodifiable Set<ModResource> getRegisteredEntrypoints() {
		return Collections.unmodifiableSet(this.registeredEntrypoints.keySet());
	}
}
