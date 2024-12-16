package io.github.dueris.eclipse.plugin.util;

import joptsimple.AbstractOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializedOptionSetData implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public List<String> detectedSpecs;
	public Map<String, String> detectedOptions;
	public Map<String, List<String>> optionsToArguments;
	public Map<String, String> recognizedSpecs;
	public Map<String, List<?>> defaultValues;

	public String compileString() {
		try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
			 ObjectOutputStream objectOut = new ObjectOutputStream(byteArrayOut)) {

			objectOut.writeObject(this);
			objectOut.flush();
			return Base64.getEncoder().encodeToString(byteArrayOut.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] compileBytes() {
		try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
			 ObjectOutputStream objectOut = new ObjectOutputStream(byteArrayOut)) {

			objectOut.writeObject(this);
			objectOut.flush();
			return byteArrayOut.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public SerializedOptionSetData decompile(String string) {
		return decompile(Base64.getDecoder().decode(string));
	}

	public SerializedOptionSetData decompile(byte[] bytes) {
		try (ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(bytes);
			 ObjectInputStream objectIn = new ObjectInputStream(byteArrayIn)) {

			SerializedOptionSetData data = (SerializedOptionSetData) objectIn.readObject();
			this.detectedSpecs = data.detectedSpecs;
			this.detectedOptions = data.detectedOptions;
			this.optionsToArguments = data.optionsToArguments;
			this.recognizedSpecs = data.recognizedSpecs;
			this.defaultValues = data.defaultValues;
			return this;
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Unable to deserialize OptionSet!", e);
		}
	}

	public OptionSet decompile() {
		OptionSet optionSet = new OptionParser().parse();
		OptionParser parser = OptionSetUtils.parser;
		Field detectedSpecsField;
		Field detectedOptionsField;
		Field optionsToArgumentsField;
		Field recognizedSpecsField;
		Field defaultValuesField;
		try {
			detectedSpecsField = OptionSet.class.getDeclaredField("detectedSpecs");
			detectedOptionsField = OptionSet.class.getDeclaredField("detectedOptions");
			optionsToArgumentsField = OptionSet.class.getDeclaredField("optionsToArguments");
			recognizedSpecsField = OptionSet.class.getDeclaredField("recognizedSpecs");
			defaultValuesField = OptionSet.class.getDeclaredField("defaultValues");
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Unable to fetch fields from OptionSet!", e);
		}

		detectedSpecsField.setAccessible(true);
		detectedOptionsField.setAccessible(true);
		optionsToArgumentsField.setAccessible(true);
		recognizedSpecsField.setAccessible(true);
		defaultValuesField.setAccessible(true);

		Map<String, OptionSpec<?>> recognizedSpecs = parser.recognizedOptions();
		try {
			detectedSpecsField.set(optionSet, this.detectedSpecs.stream()
																.map(recognizedSpecs::get)
																.collect(Collectors.toList()));
			detectedOptionsField.set(optionSet, this.detectedOptions.keySet().stream()
																	.collect(Collectors.toMap(k -> k, recognizedSpecs::get)));

			optionsToArgumentsField.set(optionSet, this.optionsToArguments.entrySet().stream()
																		  .collect(Collectors.toMap(e -> recognizedSpecs.get(e.getKey()), Map.Entry::getValue)));

			recognizedSpecsField.set(optionSet, recognizedSpecs);

			defaultValuesField.set(optionSet, this.defaultValues);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unable to set deserialized OptionSet fields!", e);
		}
		return optionSet;
	}

	@SuppressWarnings("unchecked")
	public SerializedOptionSetData serialize(OptionSet optionSet) {
		try {
			Field detectedSpecsField = OptionSet.class.getDeclaredField("detectedSpecs");
			Field detectedOptionsField = OptionSet.class.getDeclaredField("detectedOptions");
			Field optionsToArgumentsField = OptionSet.class.getDeclaredField("optionsToArguments");
			Field recognizedSpecsField = OptionSet.class.getDeclaredField("recognizedSpecs");
			Field defaultValuesField = OptionSet.class.getDeclaredField("defaultValues");

			detectedSpecsField.setAccessible(true);
			detectedOptionsField.setAccessible(true);
			optionsToArgumentsField.setAccessible(true);
			recognizedSpecsField.setAccessible(true);
			defaultValuesField.setAccessible(true);

			this.detectedSpecs = ((List<OptionSpec<?>>) detectedSpecsField.get(optionSet))
				.stream().map(OptionSpec::options).flatMap(Collection::stream).collect(Collectors.toList());

			this.detectedOptions = ((Map<String, AbstractOptionSpec<?>>) detectedOptionsField.get(optionSet))
				.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

			this.optionsToArguments = ((Map<AbstractOptionSpec<?>, List<String>>) optionsToArgumentsField.get(optionSet))
				.entrySet().stream()
				.collect(Collectors.toMap(
					e -> e.getKey().options().getFirst(),
					Map.Entry::getValue
				));

			this.recognizedSpecs = ((Map<String, AbstractOptionSpec<?>>) recognizedSpecsField.get(optionSet))
				.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

			this.defaultValues = (Map<String, List<?>>) defaultValuesField.get(optionSet);
		} catch (Throwable throwable) {
			throw new RuntimeException("Unable to serialize OptionSet!", throwable);
		}
		return this;
	}
}