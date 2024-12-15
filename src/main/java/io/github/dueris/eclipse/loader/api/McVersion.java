package io.github.dueris.eclipse.loader.api;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public record McVersion(String id, String name, @SerializedName("world_version") int worldVersion,
						@SerializedName("series_id") String seriesId,
						@SerializedName("protocol_version") int protocolVersion,
						@SerializedName("pack_version") io.github.dueris.eclipse.loader.api.McVersion.PackVersion packVersion,
						@SerializedName("build_time") String buildTime,
						@SerializedName("java_component") String javaComponent,
						@SerializedName("java_version") int javaVersion, boolean stable,
						@SerializedName("use_editor") boolean useEditor) {


	@Override
	public int worldVersion() {
		return worldVersion;
	}

	@Override
	public String seriesId() {
		return seriesId;
	}

	@Override
	public int protocolVersion() {
		return protocolVersion;
	}

	@Override
	public PackVersion packVersion() {
		return packVersion;
	}

	@Override
	public String buildTime() {
		return buildTime;
	}

	@Override
	public String javaComponent() {
		return javaComponent;
	}

	@Override
	public int javaVersion() {
		return javaVersion;
	}


	@Override
	public boolean useEditor() {
		return useEditor;
	}

	@Override
	public @NotNull String toString() {
		return "McVersion{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", worldVersion=" + worldVersion +
			", seriesId='" + seriesId + '\'' +
			", protocolVersion=" + protocolVersion +
			", packVersion=" + packVersion +
			", buildTime='" + buildTime + '\'' +
			", javaComponent='" + javaComponent + '\'' +
			", javaVersion=" + javaVersion +
			", stable=" + stable +
			", useEditor=" + useEditor +
			'}';
	}

	public record PackVersion(int resource, int data) {
		@Override
		public @NotNull String toString() {
			return "PackVersion{" +
				"resource=" + resource +
				", data=" + data +
				'}';
		}
	}
}