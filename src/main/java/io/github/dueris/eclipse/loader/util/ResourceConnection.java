package io.github.dueris.eclipse.loader.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.function.Function;
import java.util.jar.Manifest;

public final class ResourceConnection implements AutoCloseable {
	private final URLConnection connection;
	private final InputStream stream;
	private final Function<URLConnection, Manifest> manifestFunction;
	private final Function<URLConnection, CodeSource> sourceFunction;

	public ResourceConnection(final @NotNull URL url,
							  final @NotNull Function<@NotNull URLConnection, @Nullable Manifest> manifestLocator,
							  final @NotNull Function<@NotNull URLConnection, @Nullable CodeSource> sourceLocator) throws IOException {
		this.connection = url.openConnection();
		this.stream = this.connection.getInputStream();
		this.manifestFunction = manifestLocator;
		this.sourceFunction = sourceLocator;
	}

	public int contentLength() {
		return this.connection.getContentLength();
	}

	public @NotNull InputStream stream() {
		return this.stream;
	}

	public @Nullable Manifest manifest() {
		return this.manifestFunction.apply(this.connection);
	}

	public @Nullable CodeSource source() {
		return this.sourceFunction.apply(this.connection);
	}

	@Override
	public void close() throws Exception {
		this.stream.close();
	}
}
