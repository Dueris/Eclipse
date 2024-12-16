package io.github.dueris.eclipse.api.util;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public final class ClassLoaders {
	private ClassLoaders() {
	}

	@SuppressWarnings({"restriction", "unchecked"})
	public static URL @NotNull [] systemClassPaths() {
		final ClassLoader classLoader = ClassLoaders.class.getClassLoader();
		if (classLoader instanceof URLClassLoader) {
			return ((URLClassLoader) classLoader).getURLs();
		}

		if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
			try {
				final Field field = Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				final Unsafe unsafe = (Unsafe) field.get(null);

				// jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
				Field ucpField;
				try {
					ucpField = classLoader.getClass().getDeclaredField("ucp");
				} catch (final NoSuchFieldException | SecurityException e) {
					ucpField = classLoader.getClass().getSuperclass().getDeclaredField("ucp");
				}

				final long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
				final Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

				// jdk.internal.loader.URLClassPath.path
				final Field pathField = ucpField.getType().getDeclaredField("path");
				final long pathFieldOffset = unsafe.objectFieldOffset(pathField);
				final ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

				return path.toArray(new URL[0]);
			} catch (final Exception exception) {
				Logger.error(exception, "Failed to retrieve system classloader paths!");
				return new URL[0];
			}
		}

		return new URL[0];
	}
}
