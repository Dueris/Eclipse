package io.github.dueris.eclipse.loader.ember.patch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface TransformerService {
	void prepare();

	int priority(final @NotNull TransformPhase phase);

	boolean shouldTransform(final @NotNull Type type, final @NotNull ClassNode node);

	@Nullable ClassNode transform(final @NotNull Type type, final @NotNull ClassNode node, final @NotNull TransformPhase phase) throws Throwable;
}
