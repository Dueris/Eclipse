package com.dragoncommissions.mixbukkit.api.action.impl;

import com.dragoncommissions.mixbukkit.api.action.MixinAction;
import org.objectweb.asm.tree.MethodNode;

public record MActionPipeline(MixinAction... actions) implements MixinAction {

	@Override
	public void action(Class<?> owner, MethodNode method) {
		for (MixinAction action : actions) {
			action.action(owner, method);
		}
	}

}
