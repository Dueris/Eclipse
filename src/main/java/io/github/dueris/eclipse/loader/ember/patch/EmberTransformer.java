package io.github.dueris.eclipse.loader.ember.patch;

import io.github.dueris.eclipse.api.Transformer;
import io.github.dueris.eclipse.api.util.IgniteConstants;
import io.github.dueris.eclipse.loader.ember.Ember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.tinylog.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class EmberTransformer implements Transformer {
	private final Map<Class<? extends TransformerService>, TransformerService> transformers = new IdentityHashMap<>();
	private final Map<Class<? extends GamePatch>, GamePatch> patches = new IdentityHashMap<>();

	private Predicate<String> resourceExclusionFilter = path -> true;

	public EmberTransformer() {
		loadTransformers();
		loadPatches();
	}

	private void loadPatches() {
		this.patches.clear();
		final ServiceLoader<GamePatch> patchLoader = ServiceLoader.load(GamePatch.class, Ember.class.getClassLoader());
		patchLoader.forEach(this::patch);
	}

	private void loadTransformers() {
		this.transformers.clear();
		final ServiceLoader<TransformerService> serviceLoader = ServiceLoader.load(TransformerService.class, Ember.class.getClassLoader());
		serviceLoader.forEach(this::transformer);
	}

	@Override
	public void patch(GamePatch patch) {
		this.patches.put(patch.getClass(), patch);
	}

	@Override
	public void transformer(TransformerService service) {
		this.transformers.put(service.getClass(), service);
	}

	@Override
	public <T extends TransformerService> @Nullable T getTransformer(final @NotNull Class<T> transformer) {
		return transformer.cast(this.transformers.get(transformer));
	}

	@Override
	public <T extends GamePatch> @Nullable T getPatch(final @NotNull Class<T> patch) {
		return patch.cast(this.patches.get(patch));
	}

	public void addResourceExclusion(final @NotNull Predicate<String> predicate) {
		this.resourceExclusionFilter = predicate;
	}

	public @NotNull @UnmodifiableView Collection<TransformerService> transformers() {
		return Collections.unmodifiableCollection(this.transformers.values());
	}

	public byte @NotNull [] transform(final @NotNull String className, final byte @NotNull [] input, final @NotNull TransformPhase phase) {
		final String internalName = className.replace('.', '/');

		// Check if the path is excluded from transformation.
		if (!this.resourceExclusionFilter.test(internalName)) {
			Logger.debug("Skipping resource excluded class: {}", internalName);
			return input;
		}

		ClassNode node = new ClassNode(IgniteConstants.ASM_VERSION);

		final Type type = Type.getObjectType(internalName);
		if (input.length > 0) {
			final ClassReader reader = new ClassReader(input);
			reader.accept(node, 0);
		} else {
			node.name = type.getInternalName();
			node.version = MixinEnvironment.getCompatibilityLevel().getClassVersion();
			node.superName = "java/lang/Object";
		}

		final List<TransformerService> transformers = this.order(phase);
		boolean transformed = false;
		{
			// Before applying transformers, we add patches, since they have higher priorities.
			for (final GamePatch patch : patches.values()) {
				try {
					if (!patch.targets().contains(internalName)) continue;
					if (patch.applyPatch(node)) {
						transformed = true;
					}
				} catch (final Throwable throwable) {
					Logger.error(throwable, "Failed to patch {} with {}", type.getClassName(), patch.getClass()
						.getName());
				}
			}
			// Now we apply transformer services, like mixins and access wideners
			for (final TransformerService service : transformers) {
				try {
					// If the transformer should not transform the class, skip it.
					if (!service.shouldTransform(type, node)) continue;
					// Attempt to transform the class.
					final ClassNode transformedNode = service.transform(type, node, phase);
					if (transformedNode != null) {
						node = transformedNode;
						transformed = true;
					}
				} catch (final Throwable throwable) {
					Logger.error(throwable, "Failed to transform {} with {}", type.getClassName(), service.getClass()
						.getName());
				}
			}
		}

		// If no transformations were applied, return the original input.
		if (!transformed) return input;

		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		node.accept(writer);

		return writer.toByteArray();
	}

	private List<TransformerService> order(final @NotNull TransformPhase phase) {
		return this.transformers.values().stream()
			.filter(value -> value.priority(phase) != -1) // Filter out transformers that do not apply to the given phase.
			.sorted((first, second) -> {
				final int firstPriority = first.priority(phase);
				final int secondPriority = second.priority(phase);
				return Integer.compare(firstPriority, secondPriority);
			})
			.collect(Collectors.toList());
	}
}
