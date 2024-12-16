package io.github.dueris.eclipse.api.mod;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Objects;

public record ModMetadata(String id, String version, @NotNull List<String> mixins, @NotNull List<String> wideners,
						  boolean datapackEntry, YamlConfiguration backend) {

	public static @NotNull ModMetadata read(@NotNull YamlConfiguration yaml) {
		String id = yaml.getString("name").toLowerCase();
		String version = yaml.getString("version");
		return new ModMetadata(
			id,
			version,
			yaml.contains("mixins") ? yaml.getStringList("mixins") : List.of(),
			yaml.contains("wideners") ? yaml.getStringList("wideners") : List.of(),
			yaml.getBoolean("datapack-entry", false), yaml
		);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.version, this.mixins);
	}

	@Override
	public boolean equals(final @Nullable Object other) {
		if (this == other) return true;
		if (!(other instanceof ModMetadata that)) return false;
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
		return "ModMetadata{" +
			"id='" + id + '\'' +
			", version='" + version + '\'' +
			", mixins=" + mixins +
			", wideners=" + wideners +
			", datapackEntry=" + datapackEntry +
			", backend=" + backend +
			'}';
	}
}
