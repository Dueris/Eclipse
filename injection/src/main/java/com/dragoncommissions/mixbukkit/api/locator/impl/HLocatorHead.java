package com.dragoncommissions.mixbukkit.api.locator.impl;

import com.dragoncommissions.mixbukkit.api.locator.HookLocator;
import org.objectweb.asm.tree.InsnList;

import java.util.List;

public class HLocatorHead implements HookLocator {
	@Override
	public List<Integer> getLineNumber(InsnList insnNodes) {
		return List.of(0);
	}
}
