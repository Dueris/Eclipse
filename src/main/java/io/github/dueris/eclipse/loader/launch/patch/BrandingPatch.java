package io.github.dueris.eclipse.loader.launch.patch;

import io.github.dueris.eclipse.loader.ember.patch.GamePatch;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public class BrandingPatch extends GamePatch {
	public BrandingPatch() {
		this.appendTargets("net/minecraft/server/MinecraftServer");
	}

	@Override
	protected boolean applyPatch(@NotNull ClassNode classNode) {
		boolean applied = false;

		for (MethodNode node : classNode.methods) {
			if (node.name.equals("getServerModName") && node.desc.endsWith(")Ljava/lang/String;")) {
				ListIterator<AbstractInsnNode> it = node.instructions.iterator();

				while (it.hasNext()) {
					if (it.next().getOpcode() == Opcodes.ARETURN) {
						it.previous();
						it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PatchHooks.INTERNAL_NAME, "insertBranding", "(Ljava/lang/String;)Ljava/lang/String;", false));
						it.next();
					}
				}

				applied = true;
			}
		}

		return applied;
	}
}
