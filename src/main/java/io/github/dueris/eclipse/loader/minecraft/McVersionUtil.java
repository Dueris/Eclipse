package io.github.dueris.eclipse.loader.minecraft;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.dueris.eclipse.api.McVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class McVersionUtil {

	private McVersionUtil() {
	}

	public static @Nullable McVersion fromVersionJson(InputStream is) {
		try (JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String id = null;
			String name = null;
			int worldVersion = -1;
			String seriesId = null;
			int protocolVersion = -1;
			McVersion.PackVersion packVersion = null;
			String buildTime = null;
			String javaComponent = null;
			int javaVersion = -1;
			boolean stable = false;
			boolean useEditor = false;

			reader.beginObject();

			while (reader.hasNext()) {
				switch (reader.nextName()) {
					case "id":
						if (reader.peek() != JsonToken.STRING) {
							throw new IOException("\"id\" in version json must be a string");
						}
						id = reader.nextString();
						break;
					case "name":
						if (reader.peek() != JsonToken.STRING) {
							throw new IOException("\"name\" in version json must be a string");
						}
						name = reader.nextString();
						break;
					case "world_version":
						worldVersion = reader.nextInt();
						break;
					case "series_id":
						seriesId = reader.nextString();
						break;
					case "protocol_version":
						protocolVersion = reader.nextInt();
						break;
					case "pack_version":
						packVersion = readPackVersion(reader);
						break;
					case "build_time":
						buildTime = reader.nextString();
						break;
					case "java_component":
						javaComponent = reader.nextString();
						break;
					case "java_version":
						javaVersion = reader.nextInt();
						break;
					case "stable":
						stable = reader.nextBoolean();
						break;
					case "use_editor":
						useEditor = reader.nextBoolean();
						break;
					default:
						reader.skipValue();
				}
			}

			reader.endObject();

			return new McVersion(
				id, name, worldVersion, seriesId, protocolVersion, packVersion,
				buildTime, javaComponent, javaVersion, stable, useEditor
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static McVersion.@NotNull PackVersion readPackVersion(@NotNull JsonReader reader) throws IOException {
		int resource = -1;
		int data = -1;

		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "resource":
					resource = reader.nextInt();
					break;
				case "data":
					data = reader.nextInt();
					break;
				default:
					reader.skipValue();
			}
		}
		reader.endObject();
		return new McVersion.PackVersion(resource, data);
	}
}
