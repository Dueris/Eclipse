package io.github.dueris.eclipse.loader.ember;

import io.github.dueris.eclipse.loader.util.mrj.AbstractUrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class DynamicClassLoader extends AbstractUrlClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	DynamicClassLoader(final URL @NotNull [] urls) {
		this("dynamic", urls);
	}

	DynamicClassLoader(String name, final URL @NotNull [] urls) {
		super(name, urls, new DummyClassLoader());
	}

	@Override
	public void addURL(final @NotNull URL url) {
		super.addURL(url);
	}

	protected static final class DummyClassLoader extends ClassLoader {
		private static final Enumeration<URL> NULL_ENUMERATION = new Enumeration<URL>() {
			@Override
			public boolean hasMoreElements() {
				return false;
			}

			@Override
			public @NotNull URL nextElement() {
				throw new NoSuchElementException();
			}
		};

		static {
			ClassLoader.registerAsParallelCapable();
		}

		@Override
		protected @NotNull Class<?> loadClass(final @NotNull String name, final boolean resolve) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}

		@Override
		public @Nullable URL getResource(final @NotNull String name) {
			return null;
		}

		@Override
		public @NotNull Enumeration<URL> getResources(final @NotNull String name) throws IOException {
			return DummyClassLoader.NULL_ENUMERATION;
		}
	}
}
