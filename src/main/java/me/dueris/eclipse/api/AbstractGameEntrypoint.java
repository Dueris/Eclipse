package me.dueris.eclipse.api;

import org.tinylog.Logger;
import space.vectrix.ignite.game.GameEntrypoint;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractGameEntrypoint<T extends EntrypointImplementation> implements GameEntrypoint {
	private final Map<Class<T>, Integer> implementations = new LinkedHashMap<>();

	public void registerImplementation(Class<T> implementationClass) {
		registerImplementation(implementationClass, 100);
	}

	public void registerImplementation(Class<T> implementationClass, int priority) {
		implementations.put(implementationClass, priority);
	}

	@Override
	public void executeEntrypoint() {
		implementations.entrySet().stream()
			.sorted(Map.Entry.comparingByValue())
			.forEach(entry -> {
				Class<T> clazz = entry.getKey();
				try {
					clazz.getDeclaredConstructor().newInstance().init();
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
						 NoSuchMethodException e) {
					Logger.error("Failed to initialize implementation: {} {}", clazz.getName(), e);
					throw new RuntimeException("Unable to instantiate and call init for entrypoint, " + this.getClass().getName(), e);
				}
			});
	}
}
