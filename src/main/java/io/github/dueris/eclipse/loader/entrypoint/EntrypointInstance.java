package io.github.dueris.eclipse.loader.entrypoint;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.api.mod.ModResource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.ConfigurationSection;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EntrypointInstance<T> {
	private final String id;
	private final String methodName;
	protected final Class<T> instanceClass;
	private final Class<?>[] argumentTypes;
	private final Map<ModResource, Class<? extends T>> registeredEntrypoints = new HashMap<>();

	EntrypointInstance(String id, String methodName, Class<T> interfaceClass, Class<?>... argumentTypes) {
		this.id = id;
		this.methodName = methodName;
		this.instanceClass = verify(interfaceClass);
		this.argumentTypes = argumentTypes;
	}

	@Contract("_ -> param1")
	private @NotNull Class<T> verify(@NotNull Class<T> toVerify) {
		if (!toVerify.isInterface()) {
			throw new RuntimeException("Class instance is not an interface! " + toVerify.getName(), new IllegalClassFormatException());
		}
		return toVerify;
	}

	public void enter(Object... arguments) {
		registeredEntrypoints.forEach((modResource, entrypointClass) -> {
			try {
				Method method = entrypointClass.getDeclaredMethod(methodName, argumentTypes);
				method.setAccessible(true);
				T entrypointInstance = entrypointClass.getConstructor().newInstance();
				method.invoke(entrypointInstance, arguments);
			} catch (Throwable throwable) {
				throw new RuntimeException("Unable to enter mod, " + EclipseLoaderBootstrap.instance().engine().getContainerFromResource(modResource) + " !", throwable);
			}
		});
	}

	public String getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	void buildEntrypoints(@Nullable ConfigurationSection entrypointContainer, @NotNull ModResource resource) {
		if (entrypointContainer != null) {
			if (entrypointContainer.contains(id)) {
				String entryName = entrypointContainer.getString(id);
				try {
					Class<?> clazz = Class.forName(entryName);
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

	void prepare() {
		registeredEntrypoints.clear();
	}
}
