package me.dueris.eclipse.api;

import me.dueris.eclipse.ignite.game.GameEntrypoint;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GameEntrypointManager {
	private static final Map<Class<? extends AbstractGameEntrypoint<?>>, AbstractGameEntrypoint<?>> entrypoints = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static void registerEntrypoint(AbstractGameEntrypoint<?> entrypoint) {
		entrypoints.put((Class<? extends AbstractGameEntrypoint<?>>) entrypoint.getClass(), entrypoint);
	}

	public static void executeEntrypoint(Class<? extends AbstractGameEntrypoint<?>> entrypointClass) {
		AbstractGameEntrypoint<?> entrypoint = entrypoints.get(entrypointClass);
		if (entrypoint != null) {
			entrypoint.executeEntrypoint();
		} else {
			Logger.error("Entrypoint not registered: " + entrypointClass.getName());
		}
	}

	public static boolean entrypointExists(String id) {
		return entrypoints.values().stream().map(GameEntrypoint::id).collect(Collectors.toSet()).contains(id);
	}

	public static AbstractGameEntrypoint getById(String id) {
		return entrypoints.values().stream().filter(e -> e.id().equalsIgnoreCase(id)).findFirst().orElseThrow();
	}
}
