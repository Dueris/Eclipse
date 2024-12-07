package io.github.dueris.eclipse.loader.game;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a game locator service.
 *
 * @author vectrix
 * @since 1.0.0
 */
public interface GameLocatorService {
	/**
	 * The game locator identifier.
	 *
	 * @return the identifier
	 * @since 1.0.0
	 */
	@NotNull String id();

	/**
	 * The game locator name.
	 *
	 * @return the name
	 * @since 1.0.0
	 */
	@NotNull String name();

	/**
	 * The order to try select this locator.
	 *
	 * @return the priority
	 * @since 1.0.0
	 */
	int priority();

	/**
	 * Returns {@code true} if this locator should be used, otherwise returns
	 * {@code false}.
	 *
	 * @return whether this locator should be used
	 * @since 1.0.0
	 */
	boolean shouldApply();

	/**
	 * Applies this game locator.
	 *
	 * @param bootstrap the bootstrap
	 * @throws Throwable if there is a problem applying the locator
	 * @since 1.0.0
	 */
	void apply(final @NotNull EclipseLoaderBootstrap bootstrap) throws Throwable;

	/**
	 * Returns the game resource provider.
	 *
	 * @return the game resource provider
	 * @since 1.0.0
	 */
	@NotNull GameProvider locate();
}
