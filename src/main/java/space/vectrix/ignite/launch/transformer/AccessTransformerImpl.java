package space.vectrix.ignite.launch.transformer;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import space.vectrix.ignite.api.util.IgniteConstants;
import space.vectrix.ignite.launch.ember.TransformPhase;
import space.vectrix.ignite.launch.ember.TransformerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides the access transformer for Ignite.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class AccessTransformerImpl implements TransformerService {
	private final AccessWidener widener = new AccessWidener();
	private final AccessWidenerReader widenerReader = new AccessWidenerReader(this.widener);

	/**
	 * Adds a widener to this transformer.
	 *
	 * @param path the configuration path
	 * @throws IOException if an error occurs while reading the widener
	 * @since 1.0.0
	 */
	public void addWidener(final @NotNull Path path) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			this.widenerReader.read(reader);
		}
	}

	@Override
	public void prepare() {
	}

	@Override
	public int priority(final @NotNull TransformPhase phase) {
		// Only transform targets on the initialize phase.
		if (phase != TransformPhase.INITIALIZE) return -1;
		// This prioritizes access widener near the beginning of the transformation
		// pipeline.
		return 25;
	}

	@Override
	public boolean shouldTransform(final @NotNull Type type, final @NotNull ClassNode node) {
		// Only transform targets that need to be widened.
		return this.widener.getTargets().contains(node.name.replace('/', '.'));
	}

	@Override
	public @NotNull ClassNode transform(final @NotNull Type type, final @NotNull ClassNode node, final @NotNull TransformPhase phase) throws Throwable {
		final ClassNode writer = new ClassNode(IgniteConstants.ASM_VERSION);
		final ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(IgniteConstants.ASM_VERSION, writer, this.widener);

		node.accept(visitor);

		return writer;
	}
}
