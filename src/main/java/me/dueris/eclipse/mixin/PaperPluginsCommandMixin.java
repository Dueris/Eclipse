package me.dueris.eclipse.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.papermc.paper.command.PaperPluginsCommand;
import io.papermc.paper.plugin.provider.PluginProvider;
import me.dueris.eclipse.access.MixinPluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.TreeMap;

@Mixin(PaperPluginsCommand.class)
public abstract class PaperPluginsCommandMixin<T> extends BukkitCommand {

	@Unique
	private static final Component ECLIPSE_HEADER = Component.text("Eclipse Plugins:", TextColor.color(235, 186, 16));

	protected PaperPluginsCommandMixin(@NotNull String name) {
		super(name);
	}

	@Shadow
	private static <T> List<Component> formatProviders(TreeMap<String, PluginProvider<T>> plugins) {
		return null;
	}

	@Inject(method = "execute", at = @At("HEAD"))
	public void eclipse$newTreeMap(CommandSender sender, String currentAlias, String[] args, CallbackInfoReturnable<Boolean> cir, @Share("eclipsePlugins") @NotNull LocalRef<TreeMap<String, PluginProvider<JavaPlugin>>> eclipsePlugins) {
		eclipsePlugins.set(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
	}

	@SuppressWarnings("unchecked")
	@WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Ljava/util/TreeMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1))
	public <V, K> V eclipse$provideEclipseTreeMap(TreeMap instance, K key, V value, Operation<V> original, @Share("eclipsePlugins") @NotNull LocalRef<TreeMap<String, PluginProvider<JavaPlugin>>> eclipsePlugins) {
		TreeMap eclipseTreeMap = eclipsePlugins.get();

		String k = (String) key;
		PluginProvider<JavaPlugin> v = (PluginProvider<JavaPlugin>) value;

		if (((MixinPluginMeta) v.getMeta()).eclipse$isMixinPlugin()) {
			return (V) eclipseTreeMap.put(k, v);
		} else {
			return original.call(instance, key, value);
		}

	}

	@ModifyExpressionValue(method = "execute", at = @At(value = "INVOKE", target = "Ljava/util/TreeMap;size()I", ordinal = 1))
	public int eclipse$includeEclipseTreeMap(int original, @Share("eclipsePlugins") @NotNull LocalRef<TreeMap<String, PluginProvider<JavaPlugin>>> eclipsePlugins) {
		return original + eclipsePlugins.get().size();
	}

	@Inject(method = "execute", at = @At(value = "INVOKE", target = "Lorg/bukkit/command/CommandSender;sendMessage(Lnet/kyori/adventure/text/Component;)V", ordinal = 0, shift = At.Shift.AFTER))
	public void eclipse$sendEclipseMessage(CommandSender sender, String currentAlias, String[] args, CallbackInfoReturnable<Boolean> cir, @Share("eclipsePlugins") @NotNull LocalRef<TreeMap<String, PluginProvider<JavaPlugin>>> eclipsePlugins) {
		TreeMap<String, PluginProvider<JavaPlugin>> eclipseTreeMap = eclipsePlugins.get();

		if (!eclipseTreeMap.isEmpty()) {
			sender.sendMessage(ECLIPSE_HEADER);
		}

		for (Component component : formatProviders(eclipseTreeMap)) {
			sender.sendMessage(component);
		}
	}

}
