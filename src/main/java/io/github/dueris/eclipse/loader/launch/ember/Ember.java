package io.github.dueris.eclipse.loader.launch.ember;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.launch.EmberLauncher;
import io.github.dueris.eclipse.loader.launch.ember.transformer.EmberTransformer;
import io.github.dueris.eclipse.loader.launch.ember.transformer.TransformerService;
import joptsimple.OptionSet;
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
	private final EmberLauncher service;
	private EmberTransformer transformer;
	private EmberClassLoader loader;

	private Ember() {
		Ember.INSTANCE = this;

		this.service = new EmberLauncher();
	}

	/**
	 * The main entrypoint to launch Ember.
	 *
	 * @param optionSet the launch arguments
	 * @since 1.0.0
	 */
	public static void launch(final @NotNull OptionSet optionSet) {
		new Ember().run(optionSet);
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

	private void run(final @NotNull OptionSet optionSet) {
		// Transform context
		EclipseLoaderBootstrap.instance().gameLocator.transformContext();

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
		this.service.launch(optionSet, this.loader);
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
