package me.dueris.eclipse.ignite.api.mod;

import com.google.common.collect.ImmutableList;
import me.dueris.eclipse.api.AbstractGameEntrypoint;
import me.dueris.eclipse.api.GameEntrypointManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.tinylog.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a mod config.
 *
 * @since 1.0.0
 */
public record ModConfig(String id, String version, @NotNull List<String> mixins, @NotNull List<String> wideners,
						boolean datapackEntry) {

	@SuppressWarnings("unchecked")
	public static @NotNull ModConfig init(@NotNull YamlConfiguration yaml) {
		String id = yaml.getString("name").toLowerCase();
		String version = yaml.getString("version");

		List<Class<? extends AbstractGameEntrypoint<?>>> registryClasses = yaml.contains("entrypoint.registry")
			? yaml.getStringList("entrypoint.registry").stream()
			.map(className -> {
				try {
					return (Class<? extends AbstractGameEntrypoint<?>>) Class.forName(className);
				} catch (ClassNotFoundException e) {
					Logger.error("Class not found for registry entry: " + className);
					e.printStackTrace();
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList())
			: List.of();
		for (Class<? extends AbstractGameEntrypoint<?>> registryClass : registryClasses) {
			if (registryClass == null) continue;
			try {
				GameEntrypointManager.registerEntrypoint(registryClass.getDeclaredConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
					 InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		Map<String, String> containerMap = new HashMap<>();
		if (yaml.contains("entrypoint.container")) {
			yaml.getConfigurationSection("entrypoint.container").getKeys(false).forEach(key -> {
				String value = yaml.getString("entrypoint.container." + key);
				if (value != null) {
					containerMap.put(key, value);
				}
			});
		}
		containerMap.forEach((entrypoint, className) -> {
			if (!GameEntrypointManager.entrypointExists(entrypoint)) {
				Logger.error("No such entrypoint, '{}' exists! Skipping entrypoint for mod {}...", entrypoint, id);
				return;
			}
			try {
				GameEntrypointManager.getById(entrypoint).registerImplementation(Class.forName(className));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to locate entrypoint class, '" + className + "'!", e);
			}
		});

		return new ModConfig(
			id,
			version,
			yaml.contains("mixins") ? yaml.getStringList("mixins") : List.of(),
			yaml.contains("wideners") ? yaml.getStringList("wideners") : List.of(),
			yaml.getBoolean("datapack-entry", false)
		);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.version, this.mixins);
	}

	@Override
	public boolean equals(final @Nullable Object other) {
		if (this == other) return true;
		if (!(other instanceof ModConfig that)) return false;
		return Objects.equals(this.id, that.id)
			&& Objects.equals(this.version, that.version)
			&& Objects.equals(this.mixins, that.mixins)
			&& Objects.equals(this.wideners, that.wideners);
	}

	@Override
	public @Unmodifiable @NotNull List<String> mixins() {
		return ImmutableList.copyOf(mixins);
	}

	@Override
	public @Unmodifiable @NotNull List<String> wideners() {
		return ImmutableList.copyOf(wideners);
	}

	@Override
	public @NotNull String toString() {
		return "ModConfig(" +
			"id=" + this.id + ", " +
			"version=" + this.version + ", " +
			"mixins=" + Arrays.toString(this.mixins.toArray(new String[0])) + ", " +
			"wideners=" + Arrays.toString(this.wideners.toArray(new String[0])) + ")";
	}
}
