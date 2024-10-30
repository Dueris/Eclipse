package com.dragoncommissions.mixbukkit.agent;

import com.dragoncommissions.mixbukkit.MixBukkit;
import io.github.karlatemp.unsafeaccessor.UnsafeAccess;
import io.github.kasukusakura.jsa.JvmSelfAttach;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.lang.management.ManagementFactory;

public class JVMAttacher {

	static final UnsafeAccess UA = UnsafeAccess.getInstance();
	private final MixBukkit mixBukkit;

	public JVMAttacher(MixBukkit mixBukkit) {
		this.mixBukkit = mixBukkit;
	}

	@SneakyThrows
	public void attach() {
		LogManager.getLogger("JVM Attachment").info("Attaching JVM Instrumentation..");
		JvmSelfAttach.init(new File(System.getProperty("java.io.tmpdir")));
		MixBukkit.INSTRUMENTATION = JvmSelfAttach.getInstrumentation();

	}

	public int getCurrentPID() {
		return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

}
