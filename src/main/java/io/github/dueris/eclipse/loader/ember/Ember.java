package io.github.dueris.eclipse.loader.ember;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.github.dueris.eclipse.api.mod.ModEngine;
import io.github.dueris.eclipse.loader.EclipseLauncher;
import io.github.dueris.eclipse.loader.ember.patch.EmberTransformer;
import io.github.dueris.eclipse.loader.ember.patch.TransformerService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.tinylog.Logger;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public final class Ember {
	private static final List<URL> classPathUrls = new LinkedList<>();
	private static Ember INSTANCE;
	private final EclipseLauncher service;
	private EmberTransformer transformer;
	private EmberClassLoader loader;

	public Ember() {
		Ember.INSTANCE = this;

		this.service = EclipseLauncher.INSTANCE;
	}

	public static void appendToClassPath(Path path) {
		try {
			classPathUrls.add(path.toUri().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Url was malformed?", e);
		}
	}

	static @NotNull Ember instance() {
		if (Ember.INSTANCE == null) throw new IllegalStateException("Instance is only available after launch!");
		return Ember.INSTANCE;
	}

	@NotNull EmberTransformer transformer() {
		return this.transformer;
	}

	@NotNull EmberClassLoader loader() {
		return this.loader;
	}

	public void launchEmber(final @NotNull ModEngine mixinModEngine) {
		// Initialize the launch.
		this.service.initialize();

		// Prepare and create the transformer.
		mixinModEngine.gameProvider().prepareTransformer();
		this.transformer = (EmberTransformer) mixinModEngine.gameProvider().getTransformer();

		// Create the class loader.
		this.loader = new EmberClassLoader(this.transformer, (classPathUrls.toArray(new URL[0])));
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
		this.service.launch(this.loader);
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

		// Prepare transformers
		for (final TransformerService transformer : this.transformer.transformers()) {
			transformer.prepare();
		}
	}

	public EclipseLauncher getService() {
		return this.service;
	}
}
