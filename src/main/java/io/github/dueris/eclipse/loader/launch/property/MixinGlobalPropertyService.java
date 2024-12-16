package io.github.dueris.eclipse.loader.launch.property;

import io.github.dueris.eclipse.api.Launcher;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public final class MixinGlobalPropertyService implements IGlobalPropertyService {
	@Override
	public @NotNull IPropertyKey resolveKey(String name) {
		return new MixinStringPropertyKey(name);
	}

	private String keyString(IPropertyKey key) {
		return ((MixinStringPropertyKey) key).key;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key) {
		return (T) Launcher.getInstance().getProperties().get(keyString(key));
	}

	@Override
	public void setProperty(IPropertyKey key, Object value) {
		Launcher.getInstance().getProperties().put(keyString(key), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key, T defaultValue) {
		return (T) Launcher.getInstance().getProperties().getOrDefault(keyString(key), defaultValue);
	}

	@Override
	public String getPropertyString(IPropertyKey key, String defaultValue) {
		Object o = Launcher.getInstance().getProperties().get(keyString(key));
		return o != null ? o.toString() : defaultValue;
	}
}
