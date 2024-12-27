package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dueris.eclipse.plugin.util.FileDeleterVisitor;
import io.papermc.paper.pluginremap.PluginRemapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(PluginRemapper.class)
public class PluginRemapperMixin {
	@Shadow
	private static String PAPER_REMAPPED;

	// This is marked as mutable via wideners
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;", ordinal = 0))
	public Path eclipse$changeRemappedDirectory(Path instance, String other, @NotNull Operation<Path> original) {
		PAPER_REMAPPED = ".eclipse-remapped";
		Path remappedPath = original.call(instance, PAPER_REMAPPED).toAbsolutePath().normalize();
		try {
			Files.walkFileTree(remappedPath, new FileDeleterVisitor());
		} catch (IOException e) {
			throw new RuntimeException("Unable to clear remapped cache!", e);
		}
		return remappedPath;
	}
}
