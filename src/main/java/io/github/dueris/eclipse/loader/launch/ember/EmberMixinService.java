package io.github.dueris.eclipse.loader.launch.ember;

import io.github.dueris.eclipse.loader.launch.transformer.MixinTransformerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Provides the mixin service for Ember.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class EmberMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
	private final ReEntranceLock lock;
	private final EmberMixinContainer container;

	/**
	 * Creates a new mixin service.
	 *
	 * @since 1.0.0
	 */
	public EmberMixinService() {
		this.lock = new ReEntranceLock(1);
		this.container = new EmberMixinContainer("Ignite");
	}

	//<editor-fold desc="IMixinService">
	@Override
	public @NotNull String getName() {
		return "Ember/Ignite";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void prepare() {
	}

	@Override
	public MixinEnvironment.Phase getInitialPhase() {
		return MixinEnvironment.Phase.PREINIT;
	}

	@Override
	public void offer(final IMixinInternal internal) {
		if (internal instanceof IMixinTransformerFactory) {
			final MixinTransformerImpl transformer = Ember.instance().transformer().transformer(MixinTransformerImpl.class);
			if (transformer == null) return;

			transformer.offer((IMixinTransformerFactory) internal);
		}
	}

	@Override
	public void init() {
	}

	@Override
	public void beginPhase() {
	}

	@Override
	public void checkEnv(final @NotNull Object bootSource) {
	}

	@Override
	public String getSideName() {
		return Constants.SIDE_SERVER;
	}

	@Override
	public ILogger getLogger(final @NotNull String name) {
		return EmberMixinLogger.get(name);
	}

	@Override
	public ReEntranceLock getReEntranceLock() {
		return this.lock;
	}

	@Override
	public IClassProvider getClassProvider() {
		return this;
	}

	@Override
	public IClassBytecodeProvider getBytecodeProvider() {
		return this;
	}

	@Override
	public ITransformerProvider getTransformerProvider() {
		return this;
	}

	@Override
	public IClassTracker getClassTracker() {
		return this;
	}

	@Override
	public IMixinAuditTrail getAuditTrail() {
		return null;
	}

	@Override
	public Collection<String> getPlatformAgents() {
		return Collections.emptyList();
	}

	@Override
	public IContainerHandle getPrimaryContainer() {
		return this.container;
	}

	@Override
	public Collection<IContainerHandle> getMixinContainers() {
		return Collections.emptyList();
	}

	@Override
	public InputStream getResourceAsStream(final @NotNull String name) {
		final EmberClassLoader loader = Ember.instance().loader();
		return loader.getResourceAsStream(name);
	}

	@Override
	public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
		return MixinEnvironment.CompatibilityLevel.JAVA_8;
	}

	@Override
	public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
		return MixinEnvironment.CompatibilityLevel.JAVA_22;
	}
	//</editor-fold>

	//<editor-fold desc="IClassProvider">
	@Override
	public @NotNull URL[] getClassPath() {
		return new URL[0];
	}

	@Override
	public @NotNull Class<?> findClass(final @NotNull String name) throws ClassNotFoundException {
		return Class.forName(name, true, Ember.instance().loader());
	}

	@Override
	public @NotNull Class<?> findClass(final @NotNull String name, final boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, Ember.instance().loader());
	}

	@Override
	public @NotNull Class<?> findAgentClass(final @NotNull String name, final boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, Ember.class.getClassLoader());
	}
	//</editor-fold>

	//<editor-fold desc="IClassBytecodeProvider">
	@Override
	public @NotNull ClassNode getClassNode(final @NotNull String name) throws ClassNotFoundException, IOException {
		return this.getClassNode(name, true);
	}

	@Override
	public @NotNull ClassNode getClassNode(final @NotNull String name, final boolean runTransformers) throws ClassNotFoundException, IOException {
		return this.getClassNode(name, runTransformers, 0);
	}

	@Override
	public @NotNull ClassNode getClassNode(final @NotNull String name, final boolean runTransformers, final int readerFlags) throws ClassNotFoundException, IOException {
		if (!runTransformers) throw new IllegalStateException("ClassNodes must always be provided transformed!");

		final Ember ember = Ember.instance();
		final EmberClassLoader loader = ember.loader();
		final EmberTransformer transformer = ember.transformer();

		final MixinTransformerImpl mixinTransformer = transformer.transformer(MixinTransformerImpl.class);
		if (mixinTransformer == null) throw new ClassNotFoundException("Mixin transformer is not available!");

		final String canonicalName = name.replace('/', '.');
		final String internalName = name.replace('.', '/');

		final @Nullable EmberClassLoader.ClassData entry = loader.classData(canonicalName, TransformPhase.MIXIN);
		if (entry == null) throw new ClassNotFoundException(canonicalName);

		return mixinTransformer.classNode(canonicalName, internalName, entry.data(), readerFlags);
	}
	//</editor-fold>

	//<editor-fold desc="ITransformerProvider">
	@Override
	public Collection<ITransformer> getTransformers() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ITransformer> getDelegatedTransformers() {
		return Collections.emptyList();
	}

	@Override
	public void addTransformerExclusion(final @NotNull String name) {
	}
	//</editor-fold>

	//<editor-fold desc="IClassTracker">
	@Override
	public void registerInvalidClass(final @NotNull String name) {
	}

	@Override
	public boolean isClassLoaded(final @NotNull String name) {
		final EmberClassLoader loader = Ember.instance().loader();
		return loader.hasClass(name);
	}

	@Override
	public String getClassRestrictions(final @NotNull String name) {
		return "";
	}
	//</editor-fold>
}
