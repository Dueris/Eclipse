package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.mod.ModContainer;
import io.github.dueris.eclipse.api.mod.ModMetadata;
import io.github.dueris.eclipse.loader.ember.EmberClassLoader;
import io.github.dueris.eclipse.plugin.EclipsePlugin;
import io.github.dueris.eclipse.plugin.access.MixinPlugin;
import io.github.dueris.eclipse.plugin.access.PluginClassloaderHolder;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * JavaPlugin Mixin to ensure the correct classloader is being used when invoking the constructor
 */
@Mixin(JavaPlugin.class)
public abstract class JavaPluginMixin implements MixinPlugin {
	@Unique
	private static final Logger eclipse$logger = LogManager.getLogger(JavaPluginMixin.class);

	@Inject(method = "getPlugin", at = @At("HEAD"))
	private static <T extends JavaPlugin> void storeGetting(@NotNull Class<T> clazz, CallbackInfoReturnable<T> cir, @Share("fetchingPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		if (!(clazz.getClassLoader() instanceof EmberClassLoader)) return;
		fetchingPlugin.set(clazz.getName());
	}

	@ModifyExpressionValue(method = "getPlugin", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	private static ClassLoader returnCorrectClassloader(ClassLoader original, @Share("fetchingPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		if (!(original instanceof EmberClassLoader)) return original;
		eclipse$logger.trace("Replacing plugin classloader with correct classloader from Mixin plugin catch...");
		return ((PluginClassloaderHolder) EclipsePlugin.PLUGIN_TO_PROVIDER.get(fetchingPlugin.get())).eclipse$getPluginClassLoader();
	}

	@Inject(method = "getProvidingPlugin", at = @At("HEAD"))
	private static void storeGettingProvided(@NotNull Class<?> clazz, CallbackInfoReturnable<JavaPlugin> cir, @Share("fetchingProvidedPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		if (clazz.getClassLoader() instanceof EmberClassLoader) {
			if (JavaPlugin.class.isAssignableFrom(clazz) || clazz.getSuperclass() == JavaPlugin.class) {
				fetchingPlugin.set(clazz.getName());
			} else {
				try {
					Path path = Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI())
						.toAbsolutePath().normalize();
					for (ModContainer container : Launcher.getInstance().modEngine().containers()) {
						if (container.resource().path().toAbsolutePath().normalize().equals(path)) {
							fetchingPlugin.set(container.config().backend().getString("main"));
							break;
						}
					}
				} catch (URISyntaxException e) {
					throw new RuntimeException("URI had invalid syntax??(what)", e);
				}
			}
		}
	}

	@ModifyExpressionValue(method = "getProvidingPlugin", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	private static ClassLoader returnCorrectProvidingClassloader(ClassLoader original, @Share("fetchingProvidedPlugin") @NotNull LocalRef<String> fetchingPlugin) {
		if (!(original instanceof EmberClassLoader)) return original;
		eclipse$logger.trace("Replacing provided plugin classloader with correct classloader from Mixin plugin catch...");
		return ((PluginClassloaderHolder) EclipsePlugin.PLUGIN_TO_PROVIDER.get(fetchingPlugin.get())).eclipse$getPluginClassLoader();
	}

	@Shadow
	@NotNull
	public abstract PluginMeta getPluginMeta();

	@ModifyExpressionValue(method = "<init>()V", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
	public ClassLoader modifyInstanceof(ClassLoader original) {
		if (original instanceof ConfiguredPluginClassLoader) {
			return original;
		} else {
			PaperPluginParent.PaperServerPluginProvider runningProvider = EclipsePlugin.CURRENT_OPERATING_PROVIDER.get();
			if (runningProvider instanceof PluginClassloaderHolder holder) {
				return holder.eclipse$getPluginClassLoader();
			}

			throw new RuntimeException("Unable to locate correct class loader for plugin! Found: " + original.getClass()
				.getName());
		}
	}

	@Override
	public @Nullable ModContainer eclipse$getModContainer() {
		return Launcher.getInstance().modEngine().container(getPluginMeta().getName()).orElse(null);
	}

	@Override
	public @Nullable ModMetadata eclipse$getModConfig() {
		ModContainer container = eclipse$getModContainer();
		if (container == null) {
			return null;
		}
		return container.config();
	}

}
