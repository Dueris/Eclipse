package com.dragoncommissions.mixbukkit.api.action.impl;

import com.dragoncommissions.mixbukkit.api.action.MixinAction;
import com.dragoncommissions.mixbukkit.api.locator.HookLocator;
import com.dragoncommissions.mixbukkit.api.shellcode.LocalVarManager;
import com.dragoncommissions.mixbukkit.api.shellcode.ShellCode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public record MActionInsertShellCode(ShellCode shellCode, HookLocator hookLocator) implements MixinAction {

	@Override
	public void action(Class<?> owner, MethodNode method) {
		// Copy hookLocator.getLineNumber(method) to listHooks
		List<Integer> hooks = hookLocator.getLineNumber(method.instructions);

		LocalVarManager localVarManager = new LocalVarManager(method);

		// Hook!
		InsnList newInstructions = new InsnList();
		for (int i = 0; i < method.instructions.size(); i++) {
			if (hooks.contains(i)) {
				if (shellCode.getShellCodeInfo().calledDirectly()) {
					try {
						InsnList instructions = shellCode.generate(method, localVarManager);
						newInstructions.add(instructions);
						newInstructions.add(shellCode.popExtraStack());
					} catch (Exception e) {
						Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[!] Shell Code \"" + ChatColor.YELLOW + shellCode.getShellCodeInfo().name() + ChatColor.RED + "\" has failed generating instructions: Exception Thrown");
						e.printStackTrace();
					}
				} else {
					Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[!] Shell Code \"" + ChatColor.YELLOW + shellCode.getShellCodeInfo().name() + ChatColor.RED + "\" shouldn't be called directly (calledDirectly = false)");
				}

			}
			newInstructions.add(method.instructions.get(i));
		}
		method.instructions = newInstructions;
	}
}
