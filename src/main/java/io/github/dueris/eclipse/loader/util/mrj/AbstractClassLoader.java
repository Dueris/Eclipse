package io.github.dueris.eclipse.loader.util.mrj;

public abstract class AbstractClassLoader extends ClassLoader {
	public AbstractClassLoader(String name, ClassLoader parent) {
		super(name, parent);
	}
}
