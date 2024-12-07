package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.papermc.paper.PaperBootstrap;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PaperBootstrap.class)
public class PaperBootstrapMixin {

	@ModifyExpressionValue(method = "boot", at = @At(value = "INVOKE", target = "Lio/papermc/paper/PaperBootstrap;getStartupVersionMessages()Ljava/util/List;"))
	private static @Unmodifiable List<String> eclipse$removeStartupVersionMessages(List<String> original) {
		return List.of(); // We already printed this... lets not do it again?
	}
}
