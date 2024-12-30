package io.github.dueris.eclipse.loader.launch.transformer;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.loader.ember.patch.TransformPhase;
import io.github.dueris.eclipse.loader.ember.patch.TransformerService;
import io.github.dueris.eclipse.loader.launch.transformer.forge.ForgeAccessTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class ForgeTransformerService implements TransformerService {
	@Override
	public void prepare() {
		ForgeAccessTransformer.init(Launcher.getInstance().modEngine());
	}

	@Override
	public int priority(@NotNull TransformPhase phase) {
		if (phase != TransformPhase.INITIALIZE) return -1;
		return 35;
	}

	@Override
	public boolean shouldTransform(@NotNull Type type, @NotNull ClassNode node) {
		return ForgeAccessTransformer.transformers.containsValue(node.name.replace('/', '.'));
	}

	@Override
	public @Nullable ClassNode transform(@NotNull Type type, @NotNull ClassNode node, @NotNull TransformPhase phase) throws Throwable {
		return ForgeAccessTransformer.transformNode(node);
	}
}
