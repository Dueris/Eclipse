package me.dueris.eclipse.ignite.launch.ember;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import me.dueris.eclipse.ignite.IgniteBootstrap;
import me.dueris.eclipse.ignite.api.util.IgniteConstants;
import me.dueris.eclipse.ignite.launch.EclipseGameLocator;
import me.dueris.eclipse.ignite.launch.LaunchService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.tinylog.Logger;

import java.lang.reflect.Method;

/**
 * Represents the transformation launcher.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class Ember {
	private static Ember INSTANCE;
	private final LaunchService service;
	private EmberTransformer transformer;
	private EmberClassLoader loader;

	private Ember() {
		Ember.INSTANCE = this;

		this.service = new LaunchService();
	}

	/**
	 * The main entrypoint to launch Ember.
	 *
	 * @param arguments the launch arguments
	 * @since 1.0.0
	 */
	public static void launch(final String @NotNull [] arguments) {
		new Ember().run(arguments);
	}

	/* package */
	static @NotNull Ember instance() {
		if (Ember.INSTANCE == null) throw new IllegalStateException("Instance is only available after launch!");
		return Ember.INSTANCE;
	}

	/* package */
	@NotNull EmberTransformer transformer() {
		return this.transformer;
	}

	/* package */
	@NotNull EmberClassLoader loader() {
		return this.loader;
	}

	private void run(final String @NotNull [] arguments) {
		// Initialize the launch.
		this.service.initialize();

		// Create the transformer.
		this.transformer = new EmberTransformer();

		// Create the class loader.
		this.loader = new EmberClassLoader(this.transformer);
		Thread.currentThread().setContextClassLoader(this.loader);

		// Configure the class loader.
		this.service.configure(this.loader, this.transformer);

		// Start the mixin bootstrap.
		MixinBootstrap.init();

		// Prepare the launch.
		this.service.prepare(this.transformer);

		// Complete the mixin bootstrap.
		this.completeMixinBootstrap();

		// Initialize mixin extras.
		MixinExtrasBootstrap.init();

		// Execute the launch.
		try {
			Logger.info("Loading Minecraft {} with Eclipse version {}", ((EclipseGameLocator.EclipseGameProvider) IgniteBootstrap.instance().gameLocator.locate()).version().split("/")[0], IgniteConstants.IMPLEMENTATION_VERSION);
			this.service.launch(arguments, this.loader).call();
		} catch (final Exception exception) {
			Logger.error(exception, "Failed to launch the game!");
		}
	}

	private void completeMixinBootstrap() {
		// Move to the default phase.
		try {
			final Method method = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
			method.setAccessible(true);
			method.invoke(null, MixinEnvironment.Phase.INIT);
			method.invoke(null, MixinEnvironment.Phase.DEFAULT);
		} catch (final Exception exception) {
			Logger.error(exception, "Failed to complete mixin bootstrap!");
		}

		// Initialize the mixin transformer now mixin is in the correct state.
		for (final TransformerService transformer : this.transformer.transformers()) {
			transformer.prepare();
		}
	}
}
