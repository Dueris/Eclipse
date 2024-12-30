package io.github.dueris.eclipse.loader.launch.transformer.forge;

import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModEngine;
import io.github.dueris.eclipse.api.mod.ModMetadata;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ForgeAccessTransformer {
	public static final Map<TransformTarget, String> transformers = new EqualityCheckingLinkedHashMap<>();

	public static @NotNull ClassNode transformNode(@NotNull ClassNode node) {
		String className = node.name.replace("/", ".");
		if (transformers.containsValue(className)) {
			// A transformer exists with this class name.
			// Build transform targets to be applied
			List<TransformTarget> targets = transformers.entrySet()
				.stream()
				.filter(entry -> Objects.equals(entry.getValue(), className))
				.map(Map.Entry::getKey)
				.toList();
			Logger.trace("(Forge) Found " + targets.size() + " transformers for class: " + node.name);
			for (TransformTarget target : targets) {
				// Apply transformer
				Logger.trace("(Forge) Applying target: " + target.data() + " type: " + target.type());
				String i = "null";
				boolean applied = false;
				switch (target.type()) {
					case CLASS -> {
						i = node.name;
						node.access = transform(node.access, target);
						applied = true;
					}
					case FIELD -> {
						TransformTarget.FieldData data = (TransformTarget.FieldData) target.data();
						for (FieldNode field : node.fields) {
							if (field.name.equals(data.fieldName())) {
								field.access = transform(field.access, target);
								i = data.fieldName();
								applied = true;
								break;
							}
						}
					}
					case METHOD -> {
						TransformTarget.MethodData data = (TransformTarget.MethodData) target.data();
						for (MethodNode method : node.methods) {
							if ((method.name + method.desc).equals(data.methodDescriptor())) {
								method.access = transform(method.access, target);
								i = (method.name + method.desc);
								applied = true;
								break;
							}
						}
					}
				}

				if (applied) {
					Logger.trace("(Forge) Applied access transformer successfully on target of {} with data of {}", target.type(), i +
						(target.definition().removeFinal() ? " -f" : (target.definition().addFinal()) ? " +f" : ""));
				} else {
					throw new RuntimeException("Failed to apply transformer! Transformer: " + target);
				}
			}
		}
		return node;
	}

	public static int transform(int access, @NotNull TransformTarget transformer) {
		int newAccess = access;
		newAccess = transformer.data().modifier().apply(newAccess);
		if (transformer.definition().addFinal()) {
			newAccess = ModifierType.addFinal(newAccess);
		} else if (transformer.definition().removeFinal()) {
			newAccess = ModifierType.removeFinal(newAccess);
		}
		return newAccess;
	}

	public static void init(@NotNull ModEngine modEngine) {
		for (ModContainer container : modEngine.containers()) {
			ModMetadata metadata = container.config();
			if (metadata.backend().contains("transformer")) {
				String transformerName = metadata.backend().getString("transformer");
				try (JarFile jarFile = new JarFile(container.resource().path().toFile())) {
					if (jarFile.getJarEntry(transformerName) != null) {
						JarEntry entry = jarFile.getJarEntry(transformerName);
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
							String line;
							int l = 0;
							while ((line = reader.readLine()) != null) {
								l++;
								if (line.startsWith("#")) {
									// Comment, continue.
									continue;
								}
								String[] raw = line.split("\\s+");
								if (raw.length < 1) {
									Logger.error("An unhandled exception occurred when parsing transformer.");
									Logger.error(" - ModContainer: {}", container);
									Logger.error(" - Line: {}", line);
									throw new RuntimeException("Unknown syntax error occurred when parsing line in transformer.");
								}
								String rawM = raw[0];
								boolean addF = false;
								boolean removeF = false;
								if (rawM.endsWith("-f")) {
									removeF = true;
								} else if (rawM.endsWith("+f")) {
									addF = true;
								}
								FinalModifierDefinition finalDefinition = new FinalModifierDefinition(removeF, addF);

								if (addF || removeF) {
									rawM = rawM.substring(0, rawM.length() - 2);
								}

								ModifierType modifier = switch (rawM) {
									case "public" -> ModifierType.PUBLIC;
									case "protected" -> ModifierType.PROTECTED;
									case "default" -> ModifierType.DEFAULT;
									case "private" -> ModifierType.PRIVATE;
									default -> {
										Logger.error("Unknown modifier type '{}' in line {}", rawM, l);
										Logger.error(" - ModContainer: {}", container);
										Logger.error(" - Line: {}", line);
										throw new RuntimeException("Syntax exception occurred when parsing line in transformer");
									}
								};

								String cName = raw[1];
								if (raw.length == 3) {
									// Possible field or method transformer
									String pFoM = raw[2];
									TransformTarget target = (pFoM.contains("(") && pFoM.contains(")")) ?
										new TransformTarget(
											TransformTarget.Type.METHOD,
											new TransformTarget.MethodData(cName, pFoM, modifier),
											finalDefinition
										) : new TransformTarget(TransformTarget.Type.FIELD, new TransformTarget.FieldData(cName, pFoM, modifier), finalDefinition);
									transformers.put(target, cName);
								} else {
									// Just class transformer
									TransformTarget target = new TransformTarget(
										TransformTarget.Type.CLASS,
										new TransformTarget.ClassData(cName, modifier),
										finalDefinition
									);
									transformers.put(target, cName);
								}
							}
						}
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
