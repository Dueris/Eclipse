package io.github.dueris.eclipse.loader.launch.transformer.forge;

import org.objectweb.asm.Opcodes;

import java.util.function.Function;

public enum ModifierType {
	PUBLIC(ModifierType::makePublic),
	PROTECTED(ModifierType::makeProtected),
	DEFAULT(ModifierType::makePublic),
	PRIVATE(ModifierType::makePrivate);

	private final Function<Integer, Integer> transformer;

	ModifierType(Function<Integer, Integer> transformer) {
		this.transformer = transformer;
	}

	private static int makePublic(int i) {
		return (i & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}

	private static int makeProtected(int i) {
		if ((i & Opcodes.ACC_PUBLIC) != 0) {
			return i;
		}

		return (i & ~(Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PROTECTED;
	}

	public static int removeFinal(int i) {
		return i & ~Opcodes.ACC_FINAL;
	}

	public static int addFinal(int i) {
		return i | Opcodes.ACC_FINAL;
	}

	private static int makePrivate(int i) {
		return (i & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE;
	}

	public int apply(int i) {
		return transformer.apply(i);
	}

}
