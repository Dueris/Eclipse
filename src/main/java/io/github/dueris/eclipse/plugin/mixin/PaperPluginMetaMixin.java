package io.github.dueris.eclipse.plugin.mixin;

import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.plugin.access.MixinPluginMeta;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PaperPluginMeta.class)
public abstract class PaperPluginMetaMixin implements MixinPluginMeta {
	@Shadow
	public abstract @NotNull String getName();

	@Override
	public boolean eclipse$isMixinPlugin() {
		return Launcher.getInstance().modEngine().loaded(getName().toLowerCase());
	}
}
