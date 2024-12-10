package io.github.dueris.eclipse.loader.util.mrj;

import java.security.SecureClassLoader;

public abstract class AbstractSecureClassLoader extends SecureClassLoader {
	public AbstractSecureClassLoader(String name, ClassLoader parent) {
		super(name, parent);
	}
}
