package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.plugin.util.SerializedOptionSetData;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.bukkit.craftbukkit.Main;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(value = Main.class, priority = 1)
public class MainMixin {

	@Shadow
	public static boolean useJline;

	@Shadow
	public static boolean useConsole;

	@WrapOperation(method = "main", at = @At(value = "INVOKE", target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;"))
	private static @Nullable OptionSet eclipse$main(OptionParser instance, String[] arguments, Operation<OptionSet> original) {
		try {
			EclipseLoaderBootstrap.OPTIONSET = eclipse$deserializeOptionSet(new OptionParser().parse(), EclipseLoaderBootstrap.INSTANCE.bootstrapInfo.get("OptionSet").getAsString(), instance);
		} catch (IOException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return EclipseLoaderBootstrap.OPTIONSET;
	}

	@WrapOperation(method = "main", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"))
	private static void eclipse$silencePrintln(PrintStream instance, String x, Operation<Void> original) {
		// Shhhhh - we already printed it earlier... no need to do it again :)
	}

	@Unique
	private static OptionSet eclipse$deserializeOptionSet(OptionSet optionSet, String serializedData, OptionParser parser)
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
