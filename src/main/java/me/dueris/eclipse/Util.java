package me.dueris.eclipse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Util {

	public static void consumePaperClipList(Consumer<String> lineConsumer, JarEntry entry, JarFile jarFile) throws Throwable {
		try (final InputStream inputStream = jarFile.getInputStream(entry); final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] values = line.split("\t");

				if (values.length >= 3) {
					lineConsumer.accept(values[2]);
				}
			}
		}
	}
}
