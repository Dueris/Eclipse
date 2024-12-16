package io.github.dueris.eclipse.api.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dueris.eclipse.loader.util.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public record BootstrapEntryContext(String brand, boolean isProviderContext, Path serverPath, byte[] optionSet) {
	public static @NotNull BootstrapEntryContext read() {
		JsonObject jsonObject = IgniteConstants.GSON.fromJson(((Getter<String>) () -> {
			File contextFile = new File(Paths.get(".").resolve("cache").resolve(".eclipse")
											 .toFile(), "bootstrap.context");
			if (!contextFile.exists()) {
				throw new IllegalStateException("Unable to find bootstrap json! Did Eclipse start correctly?");
			}
			try {
				return Files.readString(contextFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException("Unable to build String contents of Bootstrap!", e);
			}
		}).get(), JsonObject.class);

		JsonArray retrievedArray = jsonObject.getAsJsonArray("optionset");
		byte[] bytes = new byte[retrievedArray.size()];
		for (int i = 0; i < retrievedArray.size(); i++) {
			bytes[i] = retrievedArray.get(i).getAsByte();
		}
		return new BootstrapEntryContext(
			jsonObject.get("brand").getAsString(),
			jsonObject.get("is_provider_context").getAsBoolean(),
			Path.of(jsonObject.get("path").getAsString()),
			bytes
		);
	}

}
