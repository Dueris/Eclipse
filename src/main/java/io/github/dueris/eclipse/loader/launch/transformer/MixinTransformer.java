package io.github.dueris.eclipse.loader.launch.transformer;

import io.github.dueris.eclipse.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.ember.patch.TransformPhase;
import io.github.dueris.eclipse.loader.ember.patch.TransformerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.ISyntheticClassRegistry;
import org.spongepowered.asm.transformers.MixinClassReader;

public final class MixinTransformer implements TransformerService {
	private IMixinTransformerFactory transformerFactory;
	private IMixinTransformer transformer;
	private ISyntheticClassRegistry registry;

	public void offer(final @NotNull IMixinTransformerFactory factory) {
		this.transformerFactory = factory;
	}

	@Override
	public void prepare() {
		if (this.transformerFactory == null) throw new IllegalStateException("Transformer factory is not available!");
		this.transformer = this.transformerFactory.createTransformer();
		this.registry = this.transformer.getExtensions().getSyntheticClassRegistry();
	}

	@Override
	public int priority(final @NotNull TransformPhase phase) {
		// We don't want mixin trying to transform targets it's already
		// transforming.
		if (phase == TransformPhase.MIXIN) return -1;
		// This prioritizes mixin in the middle of the transformation
		// pipeline.
		return 50;
	}

	@Override
	public boolean shouldTransform(final @NotNull Type type, final @NotNull ClassNode node) {
		// We want to send everything for mixin to decide.
		return true;
	}

	@Override
	public @Nullable ClassNode transform(final @NotNull Type type, final @NotNull ClassNode node, final @NotNull TransformPhase phase) throws Throwable {
		// Generate the class if it is synthetic through mixin.
		if (this.shouldGenerateClass(type)) {
			return this.generateClass(type, node) ? node : null;
		}

		// Transform the class through mixin.
		return this.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), type.getClassName(), node) ? node : null;
	}

	public @NotNull ClassNode classNode(final @NotNull String canonicalName, final @NotNull String internalName, final byte @NotNull [] input, final int readerFlags) throws ClassNotFoundException {
		if (input.length != 0) {
			final ClassNode node = new ClassNode(IgniteConstants.ASM_VERSION);
			final ClassReader reader = new MixinClassReader(input, canonicalName);
			reader.accept(node, readerFlags);
			return node;
		}

		final Type type = Type.getObjectType(internalName);
		if (this.shouldGenerateClass(type)) {
			final ClassNode node = new ClassNode(IgniteConstants.ASM_VERSION);
			if (this.generateClass(type, node)) return node;
		}

		throw new ClassNotFoundException(canonicalName);
	}

	boolean shouldGenerateClass(final @NotNull Type type) {
		return this.registry.findSyntheticClass(type.getClassName()) != null;
	}

	boolean generateClass(final @NotNull Type type, final @NotNull ClassNode node) {
		return this.transformer.generateClass(MixinEnvironment.getCurrentEnvironment(), type.getClassName(), node);
	}
}
