package me.dueris.eclipse.ignite.game;

/**
 * Represents a game entrypoint, which mods can use in their `paper-plugin` yaml
 * to define stages of entries. All registries get created immediately before server boot.
 */
public interface GameEntrypoint {
	/**
	 * Represents the ID for an entrypoint, which should match the entry defined in
	 * the YAML configuration to register this entrypoint.
	 * <p>
	 * For example, if the ID is "test", the corresponding entrypoint in the YAML
	 * would look like:
	 * <pre><code class="language-yaml">
	 * entrypoint:
	 *   - test:
	 *       priority: 42
	 * </code></pre>
	 */
	String id();

	void executeEntrypoint();
}
