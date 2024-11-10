package me.dueris.eclipse.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.dueris.eclipse.EclipsePlugin;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import me.dueris.eclipse.ignite.api.mod.ModConfig;
import me.dueris.eclipse.ignite.api.mod.Mods;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin {

	@Shadow
	@Final
	static Logger LOGGER;
	@Shadow
	@Final
	private static PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG;
	@Shadow
	@Final
	private DirectoryValidator validator;
	@Shadow
	@Final
	private PackSource packSource;
	@Shadow
	@Final
	private PackType packType;

	@Shadow
	private static String nameFromPath(Path path) {
		return null;
	}

	@Shadow
	public static void discoverPacks(Path path, DirectoryValidator symlinkFinder, BiConsumer<Path, Pack.ResourcesSupplier> callback) throws IOException {
	}

	@SuppressWarnings("unchecked")
	@WrapOperation(method = "discoverPacks", at = @At(value = "INVOKE", target = "Ljava/nio/file/DirectoryStream;iterator()Ljava/util/Iterator;"))
	private static <T extends Path> @NotNull Iterator<T> eclipse$noDirectories(DirectoryStream instance, @NotNull Operation<Iterator<T>> original) {
		Iterator<T> base = original.call(instance);
		if (!EclipsePlugin.eclipse$allowsJars) {
			return base;
		}

		LinkedList<Path> filteredBase = new LinkedList<>();
		while (base.hasNext()) {
			Path path = base.next();
			if (!path.toFile().isDirectory() && eclipse$filterJar(path)) {
				filteredBase.add(path);
			}
		}

		return (Iterator<T>) filteredBase.iterator();
	}

	/**
	 * Returns true if the path is a valid pack repository plugin
	 */
	@Unique
	private static boolean eclipse$filterJar(Path path) {
		Mods mods = IgniteBootstrap.mods();
		AtomicBoolean isValid = new AtomicBoolean(false);
		mods.containers().stream()
			.filter(p -> p.resource().path().toAbsolutePath().normalize().toString().equals(path.toAbsolutePath().normalize().toString()))
			.findFirst().ifPresentOrElse(resource -> {
				ModConfig config = resource.config();
				if (config.datapackEntry()) {
					isValid.set(true);
				}
			}, () -> LOGGER.info("Unable to locate mod in Ignite containers! : {}", path.getFileName()));
		return isValid.get();
	}

	@Inject(method = "loadPacks", at = @At("HEAD"))
	private void eclipse$loadPluginPacks(Consumer<Pack> profileAdder, CallbackInfo ci) {
		try {
			LOGGER.info("Loading plugin repositories...");
			EclipsePlugin.eclipse$allowsJars = true;
			discoverPacks(Paths.get("plugins"), this.validator, (path, packFactory) -> {
				PackLocationInfo packLocationInfo = this.eclipse$createDiscoveredFilePackInfo(path);
				Pack pack = Pack.readMetaAndCreate(packLocationInfo, packFactory, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
				if (pack != null) {
					profileAdder.accept(pack);
					LOGGER.info("Loaded plugin repository: {}", pack.getId());
				}
			});
			EclipsePlugin.eclipse$allowsJars = false;
		} catch (IOException e) {
			throw new RuntimeException("Unable to load plugins repository from Folder repo", e);
		}
	}

	/**
	 * Builds the plugin version of the pack information
	 */
	@Unique
	private @NotNull PackLocationInfo eclipse$createDiscoveredFilePackInfo(Path path) {
		String string = nameFromPath(path);
		return new PackLocationInfo("plugin/" + string, Component.literal(string), this.packSource, Optional.empty());
	}
}
