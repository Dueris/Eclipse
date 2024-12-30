package io.github.dueris.example.tests;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class ForgeAccessTest implements TestInstance {
	@Override
	public void test() throws TestFailedException {
		try {
			Field field = Util.class.getDeclaredField("DEFAULT_MAX_THREADS");
			System.out.println(field.accessFlags() + "- Util test (static field)");
			Method method = MinecraftServer.class.getDeclaredMethod("constructOrExtractCrashReport", Throwable.class);
			System.out.println(method.accessFlags() + "- MinecraftServer test (static method)");
			method = MinecraftServer.class.getDeclaredMethod("saveDebugReport", Path.class);
			System.out.println(method.accessFlags() + "- MinecraftServer test (non-static method)");
			field = MinecraftServer.class.getDeclaredField("debugCommandProfiler");
			System.out.println(field.accessFlags() + "- MinecraftServer test (non-static field)");
		} catch (NoSuchFieldException | NoSuchMethodException e) {
			throw new TestFailedException("Forge AT test failed", e);
		}
	}
}
