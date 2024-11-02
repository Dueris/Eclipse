package me.dueris.eclipse.util;

import joptsimple.AbstractOptionSpec;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class OptionSetStringSerializer {

	public static String serializeOptionSetFields(OptionSet optionSet) throws IOException, IllegalAccessException, NoSuchFieldException {
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

}
