package io.github.dueris.eclipse.loader.launch.patch;

import io.github.dueris.eclipse.loader.ember.patch.GamePatch;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class BootstrapEntrypointPatch extends GamePatch {
	public BootstrapEntrypointPatch() {
		this.appendTargets("net/minecraft/server/Bootstrap");
	}

	@Override
	protected boolean applyPatch(@NotNull ClassNode classNode) {
		boolean applied = false;

		for (MethodNode node : classNode.methods) {
			if (node.name.equals("bootStrap") && node.desc.equals("()V")) {
				ListIterator<AbstractInsnNode> it = node.instructions.iterator();

				while (it.hasNext()) {
					AbstractInsnNode insn = it.next();

					if (insn.getOpcode() == Opcodes.INVOKESTATIC &&
						insn instanceof MethodInsnNode methodInsn &&
						methodInsn.owner.equals("java/time/Instant") &&
						methodInsn.name.equals("now")) {

						it.add(new LdcInsnNode(Type.getType("Lio/github/dueris/eclipse/api/entrypoint/BootstrapInitializer;")));

						it.add(new MethodInsnNode(
							Opcodes.INVOKESTATIC,
							"io/github/dueris/eclipse/api/entrypoint/EntrypointContainer",
							"getEntrypoint",
							"(Ljava/lang/Class;)Lio/github/dueris/eclipse/api/entrypoint/EntrypointInstance;",
							false
						));

						it.add(new InsnNode(Opcodes.ICONST_0));
						it.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
						it.add(new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL,
							"io/github/dueris/eclipse/api/entrypoint/EntrypointInstance",
							"enter",
							"([Ljava/lang/Object;)V",
							false
						));

						applied = true;
						break;
					}
				}

				node.maxStack = Math.max(node.maxStack, 4);
			}
		}

		return applied;
	}
}
