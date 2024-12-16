package io.github.dueris.eclipse.loader.ember.patch;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class GamePatch {
	private final List<String> targets = new LinkedList<>();

	public List<String> targets() {
		return targets;
	}

	protected abstract boolean applyPatch(ClassNode classNode);

	protected void appendTargets(@NotNull String @NotNull ... targets) {
		this.targets.addAll(Arrays.asList(targets));
	}
}
