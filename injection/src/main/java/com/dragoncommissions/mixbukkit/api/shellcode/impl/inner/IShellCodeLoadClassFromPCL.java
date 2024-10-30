package com.dragoncommissions.mixbukkit.api.shellcode.impl.inner;

import com.dragoncommissions.mixbukkit.api.shellcode.LocalVarManager;
import com.dragoncommissions.mixbukkit.api.shellcode.ShellCode;
import com.dragoncommissions.mixbukkit.api.shellcode.ShellCodeInfo;
import com.dragoncommissions.mixbukkit.utils.ASMUtils;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.storage.SimpleProviderStorage;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ShellCodeInfo(
	name = "Load Class From Plugin Class Loader",
	description = "Load a class from the plugin class loader, and get its \"class\" instance",
	stacksContent = {"Class<T>"},
	calledDirectly = true
)
public class IShellCodeLoadClassFromPCL extends ShellCode {

	private final String name;
	private boolean isLast = false;

	public IShellCodeLoadClassFromPCL(@NotNull String className) {
		this.name = className.replace("/", ".");
	}

	public IShellCodeLoadClassFromPCL(@NotNull Class<?> clazz) {
		this.name = clazz.getName();
	}

	@Override
	@SneakyThrows
	public InsnList generate(MethodNode methodNode, LocalVarManager varManager) {
		InsnList out = new InsnList();
		out.add(new LdcInsnNode(name));
		out.add(new IShellCodePushInt(1).generate()); // true

		try {
			isLast = ((ArrayList) ((SimpleProviderStorage) LaunchEntryPointHandler.INSTANCE.getStorage().values().stream().toList().getLast()).getRegisteredProviders()).getLast() instanceof PaperPluginParent.PaperBootstrapProvider;
			// ((PluginProvider)((ArrayList)LaunchEntryPointHandler.INSTANCE.getStorage().values().stream().toList().getFirst().getRegisteredProviders()).getFirst()).createInstance();
			out.add(new IShellCodeFieldAccess(LaunchEntryPointHandler.class.getField("INSTANCE"), false).generate());
			out.add(new IShellCodeMethodInvoke(LaunchEntryPointHandler.class.getMethod("getStorage")).generate());
			out.add(new IShellCodeMethodInvoke(Map.class.getMethod("values")).generate());
			out.add(new IShellCodeMethodInvoke(Collection.class.getMethod("stream")).generate());
			out.add(new IShellCodeMethodInvoke(Stream.class.getMethod("toList")).generate());
			out.add(new IShellCodeMethodInvoke(
				List.class.getMethod(isLast ? "getLast" : "getFirst")
			).generate());
			out.add(new TypeInsnNode(Opcodes.CHECKCAST, "io/papermc/paper/plugin/storage/SimpleProviderStorage"));
			out.add(new IShellCodeMethodInvoke(SimpleProviderStorage.class.getMethod("getRegisteredProviders")).generate());
			out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/util/ArrayList"));
			out.add(new IShellCodeMethodInvoke(ArrayList.class.getMethod("getLast")).generate());
			out.add(new IShellCodeMethodInvoke(PluginProvider.class.getMethod("createInstance")).generate());
			out.add(new IShellCodeMethodInvoke(
				Object.class.getDeclaredMethod("getClass")
			).generate());
			out.add(new IShellCodeMethodInvoke(
				Class.class.getDeclaredMethod("getClassLoader")
			).generate());
			out.add(new IShellCodeMethodInvoke(
				Class.class.getDeclaredMethod("forName", String.class, boolean.class, ClassLoader.class)
			).generate());

		} catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}

		return out;
	}

	@ShellCodeInfo(
		name = "Field Invoke",
		description = "Call fields programmatically",
		stacksContent = {"Return value of invoked field"},
		requiredStacksContent = {"Object that calls the field", "Arguments (in order)"}
	)
	public static class IShellCodeFieldAccess extends ShellCode {

		private final Field field;
		private final boolean isSetOperation;

		public IShellCodeFieldAccess(Field field, boolean isSetOperation) {
			this.field = field;
			this.isSetOperation = isSetOperation;
		}

		@Override
		public InsnList generate(MethodNode methodNode, LocalVarManager varManager) {
			InsnList list = new InsnList();

			int opcode;
			if (Modifier.isStatic(field.getModifiers())) {
				opcode = isSetOperation ? Opcodes.PUTSTATIC : Opcodes.GETSTATIC;
			} else {
				opcode = isSetOperation ? Opcodes.PUTFIELD : Opcodes.GETFIELD;
			}

			ASMUtils.getDescriptor(field.getType());
			list.add(new FieldInsnNode(
				opcode,
				field.getDeclaringClass().getName().replace(".", "/"),
				field.getName(),
				ASMUtils.getFieldDescriptor(field.getType())
			));

			return list;
		}
	}
}
