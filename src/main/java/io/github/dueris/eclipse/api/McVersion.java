package io.github.dueris.eclipse.api;

import org.jetbrains.annotations.NotNull;

public record McVersion(String id, String name, int worldVersion, String seriesId, int protocolVersion,
						McVersion.PackVersion packVersion, String buildTime, String javaComponent, int javaVersion,
						boolean stable, boolean useEditor) {

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