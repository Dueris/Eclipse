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

@SuppressWarnings("unchecked")
public class OptionSetStringSerializer {

	public static String serializeOptionSet(OptionSet optionSet) throws IOException, IllegalAccessException, NoSuchFieldException {
		SerializedOptionSetData data = new SerializedOptionSetData();

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

		data.detectedSpecs = ((List<OptionSpec<?>>) detectedSpecsField.get(optionSet))
			.stream().map(OptionSpec::options).flatMap(Collection::stream).collect(Collectors.toList());

		data.detectedOptions = ((Map<String, AbstractOptionSpec<?>>) detectedOptionsField.get(optionSet))
			.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

		data.optionsToArguments = ((Map<AbstractOptionSpec<?>, List<String>>) optionsToArgumentsField.get(optionSet))
			.entrySet().stream()
			.collect(Collectors.toMap(
				e -> e.getKey().options().get(0),
				Map.Entry::getValue
			));

		data.recognizedSpecs = ((Map<String, AbstractOptionSpec<?>>) recognizedSpecsField.get(optionSet))
			.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

		data.defaultValues = (Map<String, List<?>>) defaultValuesField.get(optionSet);

		try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
			 ObjectOutputStream objectOut = new ObjectOutputStream(byteArrayOut)) {

			objectOut.writeObject(data);
			objectOut.flush();
			return Base64.getEncoder().encodeToString(byteArrayOut.toByteArray());
		}
	}

	public static OptionSet deserializeOptionSet(OptionSet optionSet, String serializedData, OptionParser parser)
		throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

		byte[] data = Base64.getDecoder().decode(serializedData);
		SerializedOptionSetData deserializedData;
		try (ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(data);
			 ObjectInputStream objectIn = new ObjectInputStream(byteArrayIn)) {

			deserializedData = (SerializedOptionSetData) objectIn.readObject();
		}

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

		Map<String, OptionSpec<?>> recognizedSpecs = parser.recognizedOptions();
		detectedSpecsField.set(optionSet, deserializedData.detectedSpecs.stream()
			.map(recognizedSpecs::get).collect(Collectors.toList()));

		detectedOptionsField.set(optionSet, deserializedData.detectedOptions.keySet().stream()
			.collect(Collectors.toMap(k -> k, recognizedSpecs::get)));

		optionsToArgumentsField.set(optionSet, deserializedData.optionsToArguments.entrySet().stream()
			.collect(Collectors.toMap(e -> recognizedSpecs.get(e.getKey()), Map.Entry::getValue)));

		recognizedSpecsField.set(optionSet, recognizedSpecs);

		defaultValuesField.set(optionSet, deserializedData.defaultValues);
		return optionSet;
	}
}
