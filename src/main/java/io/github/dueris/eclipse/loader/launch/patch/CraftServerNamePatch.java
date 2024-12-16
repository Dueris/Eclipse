package io.github.dueris.eclipse.loader.launch.patch;

import io.github.dueris.eclipse.loader.ember.patch.GamePatch;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class CraftServerNamePatch extends GamePatch {
	public CraftServerNamePatch() {
		this.appendTargets("org/bukkit/craftbukkit/CraftServer");
	}

	@Override
	protected boolean applyPatch(@NotNull ClassNode classNode) {
		boolean applied = false;

		for (MethodNode node : classNode.methods) {
			if (node.name.equals("getName") && node.desc.endsWith(")Ljava/lang/String;")) {
				ListIterator<AbstractInsnNode> it = node.instructions.iterator();

				while (it.hasNext()) {
					AbstractInsnNode insn = it.next();

					if (insn.getOpcode() == Opcodes.ARETURN) {
						it.previous();
						it.remove();

						it.add(new MethodInsnNode(
							Opcodes.INVOKESTATIC,
							"net/minecraft/server/MinecraftServer",
							"getServer",
							"()Lnet/minecraft/server/MinecraftServer;",
							false
						));
						it.add(new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL,
							"net/minecraft/server/MinecraftServer",
							"getServerModName",
							"()Ljava/lang/String;",
							false
						));

						it.add(new InsnNode(Opcodes.ARETURN));
						applied = true;
						break;
					}
				}
			}
		}

		return applied;
	}
}
