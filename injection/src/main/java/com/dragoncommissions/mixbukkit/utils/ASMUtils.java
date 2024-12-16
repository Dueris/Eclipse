package com.dragoncommissions.mixbukkit.utils;

import com.dragoncommissions.mixbukkit.MixBukkit;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.inner.IShellCodeLoadClassFromPCL;
import com.dragoncommissions.mixbukkit.api.shellcode.impl.inner.IShellCodeMethodInvoke;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.Opcode;
import lombok.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ASMUtils {


	public static String toDescriptorTypeName(String type) {
		switch (type) {
			case "byte":
				return "B";
			case "char":
				return "C";
			case "double":
				return "D";
			case "float":
				return "F";
			case "int":
				return "I";
			case "long":
				return "J";
			case "short":
				return "S";
			case "boolean":
				return "Z";
			case "void":
				return "V";
			default:
				if (type.endsWith("[]")) {
					String s = toDescriptorTypeName(type.substring(0, type.length() - 2));
					return "[" + s;
				}
				String clazzType = type.replace('.', '/');
				if (type.startsWith("[") && type.endsWith(";")) {
					return clazzType;
				}
				return "L" + clazzType + ";";
		}
	}

	public static String getDescriptor(Class<?> returnType, Class<?>... arguments) {
		StringBuilder out = new StringBuilder("(");
		for (Class<?> argument : arguments) {
			out.append(toDescriptorTypeName(argument.getName()));
		}
		out.append(")").append(toDescriptorTypeName(returnType.getName()));
		return out.toString();
	}

	public static String getFieldDescriptor(Class<?> fieldType) {
		return toDescriptorTypeName(fieldType);
	}

	private static String toDescriptorTypeName(Class<?> type) {
		if (type.isArray()) {
			return '[' + toDescriptorTypeName(type.getComponentType());
		} else if (type == void.class) {
			return "V";
		} else if (type == boolean.class) {
			return "Z";
		} else if (type == byte.class) {
			return "B";
		} else if (type == char.class) {
			return "C";
		} else if (type == double.class) {
			return "D";
		} else if (type == float.class) {
			return "F";
		} else if (type == int.class) {
			return "I";
		} else if (type == long.class) {
			return "J";
		} else if (type == short.class) {
			return "S";
		} else {
			return 'L' + type.getName().replace('.', '/') + ';';
		}
	}

	@SneakyThrows
	public static AbstractInsnNode loadVar(Class<?> type, int varNumber) {
		String result = "LOAD";
		if (type == byte.class) result = "I" + result;
		else if (type == char.class) result = "I" + result;
		else if (type == double.class) result = "D" + result;
		else if (type == float.class) result = "F" + result;
		else if (type == int.class) result = "I" + result;
		else if (type == long.class) result = "L" + result;
		else if (type == short.class) result = "I" + result;
		else if (type == boolean.class) result = "I" + result;
		else result = "A" + result;
		try {
			return new VarInsnNode(((int) Opcode.class.getField(result).get(null)), varNumber);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	@SneakyThrows
	public static AbstractInsnNode genReturnNode(Class<?> type) {
		String result = "RETURN";
		if (type == byte.class) result = "I" + result;
		else if (type == char.class) result = "I" + result;
		else if (type == double.class) result = "D" + result;
		else if (type == float.class) result = "F" + result;
		else if (type == int.class) result = "I" + result;
		else if (type == long.class) result = "L" + result;
		else if (type == short.class) result = "I" + result;
		else if (type == boolean.class) result = "I" + result;
		else result = "A" + result;
		try {
			return new InsnNode(((int) Opcode.class.getField(result).get(null)));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	@SneakyThrows
	public static ClassNode toClassNode(CtClass ctClass) {
		ClassNode node = new ClassNode();
		ClassReader reader = null;
		try {
			reader = new ClassReader(ctClass.toBytecode());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CannotCompileException e) {
			throw new RuntimeException(e);
		}
		reader.accept(node, 0);
		return node;
	}

	@SneakyThrows
	public static ClassNode toClassNode(byte[] bytecode) {
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytecode);
		reader.accept(node, 0);
		return node;
	}

	@SneakyThrows
	public static byte[] fromClassNode(ClassNode node) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		try {
			node.accept(writer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		if (MixBukkit.WRITE_TRANSFORMED_CLASS) {
			try {
				Path mixinOut = Paths.get("cache/mixins/output");
				if (!mixinOut.toFile().exists()) {
					mixinOut.toFile().mkdirs();
				}
				File outFile = new File(mixinOut.toFile(), node.sourceFile + ".class");
				FileOutputStream outputStream = new FileOutputStream(outFile);
				outputStream.write(writer.toByteArray());
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return writer.toByteArray();
	}


	@SneakyThrows
	public static AbstractInsnNode pushInt(int value) {
		if (value == -1) {
			return new InsnNode(Opcode.ICONST_M1);
		}
		if (value <= 5 && value >= 0) {
			return new InsnNode(value + 3);
		}
		if (value < 255) {
			return new IntInsnNode(Opcode.BIPUSH, value);
		}
		return new IntInsnNode(Opcode.SIPUSH, value);
	}

	public static Class<?> getObjectedType(Class<?> type) {
		if (type == byte.class) return Byte.class;
		else if (type == char.class) return Character.class;
		else if (type == double.class) return Double.class;
		else if (type == float.class) return Float.class;
		else if (type == int.class) return Integer.class;
		else if (type == long.class) return Long.class;
		else if (type == short.class) return Short.class;
		else if (type == boolean.class) return Boolean.class;
		return type;
	}

	public static String getObjectedTypeName(Class<?> type) {
		if (type == byte.class) return Byte.class.getName().replace(".", "/");
		else if (type == char.class) return Character.class.getName().replace(".", "/");
		else if (type == double.class) return Double.class.getName().replace(".", "/");
		else if (type == float.class) return Float.class.getName().replace(".", "/");
		else if (type == int.class) return Integer.class.getName().replace(".", "/");
		else if (type == long.class) return Long.class.getName().replace(".", "/");
		else if (type == short.class) return Short.class.getName().replace(".", "/");
		else if (type == boolean.class) return Boolean.class.getName().replace(".", "/");
		else return "";
	}

	public static InsnList generateGetClassNode(Class<?> type) {
		if (type == byte.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Byte.class.getName()
																					 .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == char.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Character.class.getName()
																						  .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == double.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Double.class.getName()
																					   .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == float.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Float.class.getName()
																					  .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == int.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Integer.class.getName()
																						.replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == long.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Long.class.getName()
																					 .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == short.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Short.class.getName()
																					  .replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		else if (type == boolean.class)
			return ASMUtils.asInsnList(new FieldInsnNode(Opcode.GETSTATIC, Boolean.class.getName()
																						.replace(".", "/"), "TYPE", "Ljava/lang/Class;"));
		return new IShellCodeLoadClassFromPCL(type).generate();
	}

	public static InsnList asInsnList(AbstractInsnNode... nodes) {
		InsnList list = new InsnList();
		for (AbstractInsnNode node : nodes) {
			list.add(node);
		}
		return list;
	}

	public static InsnList castToObject(int varLocation, Class<?> type) {
		InsnList list = new InsnList();
		AbstractInsnNode insnNode = loadVar(type, varLocation);
		list.add(insnNode);
		String objectedType = getObjectedTypeName(type);
		if (objectedType.equals("")) return list;
		list.add(new MethodInsnNode(Opcode.INVOKESTATIC, objectedType, "valueOf", getDescriptor(getObjectedType(type), type)));
		return list;
	}

	@SneakyThrows
	public static Class<?> descriptorToClass(String s) {
		Class<?> type = switch (s) {
			case "Z" -> boolean.class;
			case "C" -> char.class;
			case "B" -> byte.class;
			case "S" -> short.class;
			case "I" -> int.class;
			case "J" -> long.class;
			case "F" -> float.class;
			case "D" -> double.class;
			case "V" -> void.class;
			default -> null;
		};
		if (s.startsWith("L")) {
			try {
				type = Class.forName(s.substring(1, s.length() - 1).replace("/", "."));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return type;
	}

	public static Class<?> getReturnType(String descriptor) {
		String s = descriptor.split("\\)")[1];
		return descriptorToClass(s);
	}

	@SneakyThrows
	public static InsnList cast(Class<?> type) {
		InsnList out = new InsnList();
		Class<?> objectedType = ASMUtils.getObjectedType(type);
		if (objectedType != type) {
			out.add(new TypeInsnNode(Opcode.CHECKCAST, objectedType.getName().replace(".", "/")));
			try {
				out.add(new IShellCodeMethodInvoke(objectedType.getDeclaredMethod(type.getName() + "Value")).generate());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		} else {
			out.add(new TypeInsnNode(Opcode.CHECKCAST, objectedType.getName().replace(".", "/")));
		}
		return out;
	}

	public static int getLatestVarNumber(InsnList list) {
		int out = 5;
		for (AbstractInsnNode insnNode : list) {
			if (insnNode instanceof VarInsnNode) {
				int var = ((VarInsnNode) insnNode).var;
				out = Math.max(var, out);
			}
		}
		return out;
	}

}
