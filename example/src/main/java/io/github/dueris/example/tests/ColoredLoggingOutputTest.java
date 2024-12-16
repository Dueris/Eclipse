package io.github.dueris.example.tests;

import io.github.dueris.example.EclipseExample;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public class ColoredLoggingOutputTest implements TestInstance {
	@Override
	public void test() throws TestFailedException {
		EclipseExample plugin = JavaPlugin.getPlugin(EclipseExample.class);
		printLine("TEST", System.out);
		// We don't test chat colors bc its paper... this has been deprecated for years... (i also don't think they work normally...)
		plugin.getServer().getConsoleSender()
			  .sendMessage(Component.text("TEST_(Component)").color(TextColor.color(0xA852F)));
	}

	private void printLine(String line, @NotNull PrintStream stream) {
		stream.print(line + "\n");
	}
}
