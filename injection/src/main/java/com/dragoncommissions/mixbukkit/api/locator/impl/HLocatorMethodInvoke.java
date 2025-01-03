package com.dragoncommissions.mixbukkit.api.locator.impl;

import com.dragoncommissions.mixbukkit.api.locator.HookLocator;
import com.dragoncommissions.mixbukkit.utils.ASMUtils;
import com.dragoncommissions.mixbukkit.utils.PostPreState;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class HLocatorMethodInvoke implements HookLocator {

	private final String owner;
	private final String desc;
	private final String name;
	private final PostPreState state;
	private final Predicate<Integer> filter;

	public HLocatorMethodInvoke(@NotNull Method method, PostPreState state, Predicate<Integer> filter) {
		owner = method.getDeclaringClass().getName().replace(".", "/");
		desc = ASMUtils.getDescriptor(method.getReturnType(), method.getParameterTypes());
		name = method.getName();
		this.filter = filter;
		this.state = state;
	}

	public HLocatorMethodInvoke(@NotNull Class<?> owner, @NotNull Method method, PostPreState state, Predicate<Integer> filter) {
		this.owner = owner.getName().replace(".", "/");
		desc = ASMUtils.getDescriptor(method.getReturnType(), method.getParameterTypes());
		name = method.getName();
		this.filter = filter;
		this.state = state;
	}

	@Override
	public List<Integer> getLineNumber(@NotNull InsnList insnList) {
		int amount = 0;
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < insnList.size(); i++) {
			if (insnList.get(i) instanceof MethodInsnNode insnNode) {
				if (insnNode.owner.equals(owner) && insnNode.name.equals(name) && insnNode.desc.equals(desc) && filter.test(amount++)) {
					out.add(i + (state == PostPreState.POST ? 1 : 0));
				}
			}
		}
		return out;
	}
}
