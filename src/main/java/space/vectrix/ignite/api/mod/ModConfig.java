package space.vectrix.ignite.api.mod;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a mod config.
 *
 * @since 1.0.0
 */
public record ModConfig(String id, String version, @NotNull List<String> mixins, @NotNull List<String> wideners) {

	public static @NotNull ModConfig init(@NotNull YamlConfiguration yaml) {
		String id = yaml.getString("name").toLowerCase();
		String version = yaml.getString("version");
		return new ModConfig(id, version, yaml.contains("mixins") ? yaml.getStringList("mixins") : List.of(), yaml.contains("wideners") ? yaml.getStringList("wideners") : List.of());
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
