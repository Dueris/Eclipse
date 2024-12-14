package io.github.dueris.eclipse.loader.agent.patch;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

public abstract class GamePatch implements ClassFileTransformer {
	private final List<String> targets;

	public GamePatch(String[] targets) {
		this.targets = Arrays.stream(targets).toList();
	}

	abstract public boolean applyBrandingPatch(@NotNull ClassNode classNode);

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
		if (!this.targets.contains(className)) return null;
		try {
			ClassReader classReader = new ClassReader(classFileBuffer);
			ClassNode classNode = new ClassNode();

			classReader.accept(classNode, 0);

			if (applyBrandingPatch(classNode)) {
				ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				classNode.accept(classWriter);

				return classWriter.toByteArray();
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to apply game patch!", e);
		}
		return null;
	}

}
