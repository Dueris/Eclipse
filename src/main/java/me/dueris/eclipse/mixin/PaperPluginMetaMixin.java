package me.dueris.eclipse.mixin;

import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import me.dueris.eclipse.access.MixinPluginMeta;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PaperPluginMeta.class)
public abstract class PaperPluginMetaMixin implements MixinPluginMeta {
	@Shadow
	public abstract @NotNull String getName();

	@Override
	public boolean eclipse$isMixinPlugin() {
		return IgniteBootstrap.mods().loaded(getName().toLowerCase());
	}
}
