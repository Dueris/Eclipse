package io.github.dueris.eclipse.loader.launch.transformer.forge;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

public class EqualityCheckingLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

	@Override
	public V put(@NotNull K key, @NotNull V value) {
		for (K existingKey : keySet()) {
			if (existingKey.equals(key)) {
				return super.put(existingKey, value);
			}
		}

		return super.put(key, value);
	}

	@Override
	public boolean containsKey(Object key) {
		for (K existingKey : keySet()) {
			if (existingKey.equals(key)) {
				return true;
			}
		}
		return false;
	}
}