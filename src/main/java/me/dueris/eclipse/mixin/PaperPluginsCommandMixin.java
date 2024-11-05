package me.dueris.eclipse.mixin;

import io.papermc.paper.command.PaperPluginsCommand;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import me.dueris.eclipse.access.MixinPluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.TreeMap;

@Mixin(PaperPluginsCommand.class)
public abstract class PaperPluginsCommandMixin<T> extends BukkitCommand {

	@Unique
	private static final Component ECLIPSE_HEADER = Component.text("Eclipse Plugins:", TextColor.color(235, 186, 16));
	@Shadow
	@Final
	private static Component PAPER_HEADER;
	@Shadow
	@Final
	private static Component BUKKIT_HEADER;

	@Shadow
	private static <T> List<Component> formatProviders(TreeMap<String, PluginProvider<T>> plugins) {
		return null;
	}

	protected PaperPluginsCommandMixin(@NotNull String name) {
		super(name);
	}

	/**
	 * @author Dueris
	 * @reason Cleanup this method and add Eclipse plugins :)
	 */
	@Overwrite
	public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
		if (!this.testPermission(sender)) return true;

		TreeMap<String, PluginProvider<JavaPlugin>> eclipsePlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		TreeMap<String, PluginProvider<JavaPlugin>> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		TreeMap<String, PluginProvider<JavaPlugin>> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


		for (PluginProvider<JavaPlugin> provider : LaunchEntryPointHandler.INSTANCE.get(Entrypoint.PLUGIN).getRegisteredProviders()) {
			PluginMeta configuration = provider.getMeta();

			if (configuration instanceof MixinPluginMeta mixinPluginMeta && mixinPluginMeta.eclipse$isMixinPlugin()) {
				eclipsePlugins.put(configuration.getDisplayName(), provider);
			} else if (provider instanceof SpigotPluginProvider) {
				spigotPlugins.put(configuration.getDisplayName(), provider);
			} else if (provider instanceof PaperPluginParent.PaperServerPluginProvider) {
				paperPlugins.put(configuration.getDisplayName(), provider);
			}
		}

		Component infoMessage = Component.text("Server Plugins (%s):".formatted(eclipsePlugins.size() + paperPlugins.size() + spigotPlugins.size()), NamedTextColor.WHITE);

		sender.sendMessage(infoMessage);

		if (!eclipsePlugins.isEmpty()) {
			sender.sendMessage(ECLIPSE_HEADER);
		}

		for (Component component : formatProviders(eclipsePlugins)) {
			sender.sendMessage(component);
		}

		if (!paperPlugins.isEmpty()) {
			sender.sendMessage(PAPER_HEADER);
		}

		for (Component component : formatProviders(paperPlugins)) {
			sender.sendMessage(component);
		}

		if (!spigotPlugins.isEmpty()) {
			sender.sendMessage(BUKKIT_HEADER);
		}

		for (Component component : formatProviders(spigotPlugins)) {
			sender.sendMessage(component);
		}

		return true;
	}

}
