package me.dueris.eclipse.mixin;

import io.papermc.paper.plugin.provider.source.DirectoryProviderSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.vectrix.ignite.IgniteBootstrap;

import java.nio.file.Path;
import java.util.List;

@Mixin(DirectoryProviderSource.class)
public class DirectoryProviderSourceMixin {

	@Inject(method = "lambda$prepareContext$1", at = @At("HEAD"), cancellable = true)
	private static void eclipse$dontLoadEclipse(List files, @NotNull Path path, CallbackInfo ci) {
		if (IgniteBootstrap.ROOT_ABSOLUTE.toString().equalsIgnoreCase(path.toAbsolutePath().toString())) {
			ci.cancel();
		}
	}
}
