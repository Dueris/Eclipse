package io.github.dueris.eclipse.loader.minecraft;

import io.github.dueris.eclipse.api.GameLibrary;
import io.github.dueris.eclipse.api.Launcher;
import io.github.dueris.eclipse.api.McVersion;
import io.github.dueris.eclipse.api.Transformer;
import io.github.dueris.eclipse.api.game.GameProvider;
import io.github.dueris.eclipse.api.util.BootstrapEntryContext;
import io.github.dueris.eclipse.loader.MixinJavaAgent;
import io.github.dueris.eclipse.loader.ember.patch.EmberTransformer;
import io.github.dueris.eclipse.loader.util.LaunchException;
import io.github.dueris.eclipse.plugin.access.EclipseMain;
import io.github.dueris.eclipse.plugin.util.OptionSetUtils;
import joptsimple.OptionSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

public class MinecraftGameProvider implements GameProvider {
	private final String gameBrand;
	private final McVersion version;
	private final String mainClass;
	private final OptionSet optionSet;
	private final PaperclipJar paperclipJar;
	private EmberTransformer transformer;

	public MinecraftGameProvider() {
		Map<String, Object> properties = Launcher.getInstance().getProperties();
		Path gameJarPath = (Path) properties.get("gamejar");
		try {
			MixinJavaAgent.appendToClassPath(gameJarPath);
		} catch (final IOException exception) {
			throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
		}

		try {
			PaperclipJar paperclipJar = new PaperclipJar(gameJarPath.toFile());
			BootstrapEntryContext context = Launcher.getInstance().entryContext();
			this.gameBrand = context.brand();
			this.version = paperclipJar.mcVer();
			this.mainClass = paperclipJar.getMainClass();
			this.optionSet = OptionSetUtils.Serializer.deserialize(context.optionSet());
			this.paperclipJar = paperclipJar;
		} catch (IOException e) {
			throw new RuntimeException("Unable to build paperclip jar!", e);
		}
	}

	@Override
	public Stream<GameLibrary> getLibraries() {
		return paperclipJar.gameRecord.libraries();
	}

	@Override
	public Path getLaunchJar() {
		return paperclipJar.gameRecord.gamePath();
	}

	@Override
	public String getGameId() {
		return "minecraft";
	}

	@Override
	public String getGameName() {
		return gameBrand;
	}

	@Override
	public String getEntrypoint() {
		return mainClass;
	}

	@Override
	public Path getLaunchDirectory() {
		return Paths.get(".");
	}

	public void launch(ClassLoader loader) {
		try {
			final Path gameJar = (Path) Launcher.getInstance().getProperties().get("gamejar");
			final String gameTarget = this.paperclipJar.getMainClass();
			if (gameJar != null && Files.exists(gameJar)) {
				Object instance = Class.forName(gameTarget, true, loader).getConstructor().newInstance();
				EclipseMain.class.getMethod("eclipse$main", OptionSet.class).invoke(instance, optionSet);
			} else {
				throw new IllegalStateException("No game jar was found to launch!");
			}
		} catch (Throwable throwable) {
			throw new LaunchException("Unable to launch Minecraft server!", throwable);
		}
	}

	@Override
	public void prepareTransformer() {
		this.transformer = new EmberTransformer();
	}

	@Override
	public Transformer getTransformer() {
		return this.transformer;
	}

	@Override
	public OptionSet getArguments() {
		return optionSet;
	}

	@Override
	public McVersion getVersion() {
		return version;
	}

	public PaperclipJar getPaperclipJar() {
		return paperclipJar;
	}
}
