package io.github.dueris.eclipse.api.mod;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.tinylog.TaggedLogger;

@ApiStatus.NonExtendable
public interface ModContainer {
	@NotNull TaggedLogger logger();

	@NotNull String id();

	@NotNull String version();

	@NotNull ModResource resource();

	@NotNull ModMetadata config();
}
