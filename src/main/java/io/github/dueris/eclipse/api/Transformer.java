package io.github.dueris.eclipse.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.dueris.eclipse.loader.ember.patch.GamePatch;
import io.github.dueris.eclipse.loader.ember.patch.TransformerService;

public interface Transformer {
    /**
     * Gets the TransformerService in the registry from the class. Will return null
     * if not registered.
     * 
     * @param <T> The TransformerService instance
     * @param transformer the class of the TransformerService
     * 
     * @return The TransformerService found in the registry
     */
    <T extends TransformerService> @Nullable T getTransformer(final @NotNull Class<T> transformer);

    /**
     * Gets the GamePatch in the registry from its class. Will return null if not registered.
     * 
     * @param <T> The GamePatch instance
     * @param patch the class of the GamePatch
     * 
     * @return The GamePatch found in the registry
     */
    <T extends GamePatch> @Nullable T getPatch(final @NotNull Class<T> patch);

    /**
     * Registers a GamePatch to the classpath transformer
     * @param patch the GamePatch to register
     */
    void patch(GamePatch patch);

    /**
     * Registers a TransformerService to the classpath transformer
     * @param service the TransformerService to register
     */
    void transformer(TransformerService service);
}
