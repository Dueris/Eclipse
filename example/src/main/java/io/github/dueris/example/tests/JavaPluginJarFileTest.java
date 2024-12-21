package io.github.dueris.example.tests;

import io.github.dueris.example.EclipseExample;

import java.io.IOException;
import java.util.jar.JarFile;

public class JavaPluginJarFileTest implements TestInstance {
	@Override
	public void test() throws TestFailedException {
		EclipseExample example = EclipseExample.getPlugin(EclipseExample.class);
		try (JarFile jarFile = new JarFile(example.jarFile())) {
			LOGGER.info(jarFile.getManifest().toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
