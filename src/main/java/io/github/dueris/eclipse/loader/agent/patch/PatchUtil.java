package io.github.dueris.eclipse.loader.agent.patch;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PatchUtil {

	public static @NotNull String @NotNull [] classTargets(@NotNull Class<?>... clazz) {
		return Arrays.stream(clazz).map(Class::getName).map(n -> n.replace(".", "/")).toList().toArray(new String[]{});
	}
}
