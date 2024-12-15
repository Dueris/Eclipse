package io.github.dueris.eclipse.loader.minecraft;

import io.github.dueris.eclipse.loader.agent.IgniteAgent;
import io.github.dueris.eclipse.loader.agent.patch.BrandingPatch;
import io.github.dueris.eclipse.loader.agent.patch.GamePatch;
import io.github.dueris.eclipse.loader.game.GameTransformer;

import java.util.List;

public class MinecraftGameTransformer implements GameTransformer {
	private final List<Class<? extends GamePatch>> patches = List.of(
		BrandingPatch.class
	);

	MinecraftGameTransformer() {
	}

	@Override
	public void transformContext() {
		for (Class<? extends GamePatch> patch : patches) {
			IgniteAgent.addPatch(patch);
		}
	}
}
