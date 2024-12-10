package io.github.dueris.eclipse.loader.util.mrj;

import java.net.URL;
import java.net.URLClassLoader;

public abstract class AbstractUrlClassLoader extends URLClassLoader {
	public AbstractUrlClassLoader(String name, URL[] urls, ClassLoader parent) {
		super(name, urls, parent);
	}
}
