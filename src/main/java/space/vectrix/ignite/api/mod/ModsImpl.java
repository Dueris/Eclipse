package space.vectrix.ignite.api.mod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;
import org.tinylog.Logger;
import space.vectrix.ignite.agent.IgniteAgent;
import space.vectrix.ignite.launch.ember.EmberMixinContainer;
import space.vectrix.ignite.launch.ember.EmberMixinService;
import space.vectrix.ignite.launch.ember.EmberTransformer;
import space.vectrix.ignite.launch.transformer.AccessTransformerImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents the mod loading engine.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class ModsImpl implements Mods {
	private final ModResourceLocator resourceLocator = new ModResourceLocator();
	private final ModResourceLoader resourceLoader = new ModResourceLoader();
	private final Map<String, ModContainer> containersByConfig = new HashMap<>();
	private final Map<String, ModContainer> containers = new HashMap<>();
	private final List<ModResource> resources = new ArrayList<>();

	/**
	 * Creates a new mod loading engine.
	 *
	 * @since 1.0.0
	 */
	public ModsImpl() {
	}

	@Override
	public boolean loaded(final @NotNull String id) {
		return this.containers.containsKey(id);
	}

	@Override
	public @NotNull Optional<ModContainer> container(final @NotNull String id) {
		return Optional.ofNullable(this.containers.get(id));
	}

	@Override
	public @NotNull @UnmodifiableView List<ModResource> resources() {
		return Collections.unmodifiableList(this.resources);
	}

	@Override
	public @NotNull @UnmodifiableView Collection<ModContainer> containers() {
		return Collections.unmodifiableCollection(this.containers.values());
	}

	/**
	 * Returns {@code true} if any mod resources were located, otherwise returns
	 * {@code false}.
	 *
	 * @return whether any mod resources were located
	 * @since 1.0.0
	 */
	public boolean locateResources() {
		return this.resources.addAll(this.resourceLocator.locateResources());
	}

	/**
	 * Returns a list of resolved mod container paths.
	 *
	 * @return resolved mod container paths
	 * @since 1.0.0
	 */
	public @NotNull List<Map.Entry<String, Path>> resolveResources() {
		final List<Map.Entry<String, Path>> targetResources = new ArrayList<>();
		for (final ModContainerImpl container : this.resourceLoader.loadResources(this)) {
			final ModResource resource = container.resource();

			if (!resource.locator().equals(ModResourceLocator.LAUNCHER_LOCATOR) && !resource.locator().equals(ModResourceLocator.GAME_LOCATOR)) {
				try {
					IgniteAgent.addJar(container.resource().path());
				} catch (final IOException exception) {
					Logger.error(exception, "Unable to add container '{}' to the classpath!", container.id());
				}
			}

			this.containers.put(container.id(), container);

			final String prettyIdentifier = String.format("%s@%s", container.id(), container.version());
			targetResources.add(new AbstractMap.SimpleEntry<>(prettyIdentifier, container.resource().path()));
		}

		return targetResources;
	}

	/**
	 * Resolves the access wideners provided by the mods.
	 *
	 * @param transformer the transformer
	 * @since 1.0.0
	 */
	public void resolveWideners(final @NotNull EmberTransformer transformer) {
		final AccessTransformerImpl accessTransformer = transformer.transformer(AccessTransformerImpl.class);
		if (accessTransformer == null) return;

		for (final ModContainer container : this.containers()) {
			final ModResource resource = container.resource();

			final List<String> wideners = ((ModContainerImpl) container).config().wideners();
			if (wideners != null && !wideners.isEmpty()) {
				for (final String widener : wideners) {
					//noinspection resource
					final Path path = resource.fileSystem().getPath(widener);
					try {
						Logger.trace("Adding the access widener: {}", widener);
						accessTransformer.addWidener(path);
					} catch (final IOException exception) {
						Logger.trace(exception, "Failed to configure widener: {}", widener);
						continue;
					}

					Logger.trace("Added the access widener: {}", widener);
				}
			}
		}
	}

	/**
	 * Applies the mixin transformers provided by the mods.
	 *
	 * @since 1.0.0
	 */
	public void resolveMixins() {
		final EmberMixinService service = (EmberMixinService) MixinService.getService();
		final EmberMixinContainer handle = (EmberMixinContainer) service.getPrimaryContainer();

		// Add the mixin configurations.
		for (final ModContainer container : this.containers()) {
			final ModResource resource = container.resource();

			handle.addResource(resource.path().getFileName().toString(), resource.path());

			final List<String> mixins = ((ModContainerImpl) container).config().mixins();
			if (mixins != null && !mixins.isEmpty()) {
				for (final String config : mixins) {
					final ModContainer previous = this.containersByConfig.putIfAbsent(config, container);
					if (previous != null) {
						Logger.warn("Skipping duplicate mixin configuration: {} (in {} and {})", config, previous.id(), container.id());
						continue;
					}

					Mixins.addConfiguration(config);
				}

				Logger.trace("Added the mixin configurations: {}", String.join(", ", mixins));
			}
		}

		// Add the decorators.
		for (final Config config : Mixins.getConfigs()) {
			final ModContainer container = this.containersByConfig.get(config.getName());
			if (container == null) continue;

			final IMixinConfig mixinConfig = config.getConfig();
			mixinConfig.decorate(FabricUtil.KEY_MOD_ID, container.id());
			mixinConfig.decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST);
		}
	}
}
