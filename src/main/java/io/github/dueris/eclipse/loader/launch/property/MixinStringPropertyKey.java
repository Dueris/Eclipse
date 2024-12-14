package io.github.dueris.eclipse.loader.launch.property;

import org.spongepowered.asm.service.IPropertyKey;

import java.util.Objects;

public class MixinStringPropertyKey implements IPropertyKey {
	public final String key;

	public MixinStringPropertyKey(String key) {
		this.key = key;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MixinStringPropertyKey)) {
			return false;
		} else {
			return Objects.equals(this.key, ((MixinStringPropertyKey) obj).key);
		}
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public String toString() {
		return this.key;
	}
}
