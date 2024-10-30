package com.dragoncommissions.mixbukkit.utils.io;

import java.io.IOException;
import java.io.OutputStream;

public class BukkitErrorOutputStream extends OutputStream {


	public BukkitErrorOutputStream() {

	}

	@Override
	public void write(int b) throws IOException {
		if (b == '\n') {
			System.err.print("\u001B[0m");
		}
		System.err.write(b);
		if (b == '\n') {
			System.err.print("\u001B[31m");
		}
	}
}
