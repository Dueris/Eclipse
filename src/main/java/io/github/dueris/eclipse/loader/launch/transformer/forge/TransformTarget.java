package io.github.dueris.eclipse.loader.launch.transformer.forge;

public record TransformTarget(TransformTarget.Type type,
							  TargetData data, FinalModifierDefinition definition) {

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		TransformTarget other = (TransformTarget) obj;

		if (type != other.type) {
			return false;
		}

		if (type == Type.FIELD && data instanceof FieldData thisField && other.data instanceof FieldData otherField) {
			return thisField.className().equals(otherField.className())
				&& thisField.fieldName().equals(otherField.fieldName())
				&& thisField.modifier == otherField.modifier;
		}

		if (type == Type.CLASS && data instanceof ClassData thisClass && other.data instanceof ClassData otherClass) {
			return thisClass.className().equals(otherClass.className())
				&& thisClass.modifier == otherClass.modifier;
		}

		if (type == Type.METHOD && data instanceof MethodData thisMethod && other.data instanceof MethodData otherMethod) {
			return thisMethod.className().equals(otherMethod.className())
				&& thisMethod.methodDescriptor().equals(otherMethod.methodDescriptor());
		}

		return false;
	}

	public enum Type {
		CLASS, METHOD, FIELD
	}

	public record ClassData(String className, ModifierType modifier) implements TargetData {
	}

	public record FieldData(String className, String fieldName, ModifierType modifier) implements TargetData {
	}

	public record MethodData(String className, String methodDescriptor, ModifierType modifier) implements TargetData {
	}
}
