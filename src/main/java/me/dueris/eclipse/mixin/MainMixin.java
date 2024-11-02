package me.dueris.eclipse.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.dueris.eclipse.api.DedicatedServerInitEntrypoint;
import me.dueris.eclipse.api.GameEntrypointManager;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import me.dueris.eclipse.util.SerializedOptionSetData;
import org.bukkit.craftbukkit.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(Main.class)
public class MainMixin {

	@Inject(method = "main", at = @At("HEAD"))
	private static void executeInitEntrypoint(String[] args, CallbackInfo ci) {
		GameEntrypointManager.executeEntrypoint(DedicatedServerInitEntrypoint.class);
	}

	@WrapOperation(method = "main", at = @At(value = "INVOKE", target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;"))
	private static OptionSet eclipse$injectOptionSet(OptionParser instance, String[] arguments, Operation<OptionSet> original) {
		try {
			IgniteBootstrap.OPTIONSET = eclipse$deserializeOptionSetFields(new OptionParser().parse(), IgniteBootstrap.INSTANCE.bootstrapInfo.get("OptionSet").getAsString(), instance);
		} catch (IOException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return IgniteBootstrap.OPTIONSET;
	}

	@Unique
	private static OptionSet eclipse$deserializeOptionSetFields(OptionSet optionSet, String serializedData, OptionParser parser)
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
