package io.github.dueris.eclipse.loader.minecraft;

import io.github.dueris.eclipse.loader.EclipseLoaderBootstrap;
import io.github.dueris.eclipse.loader.agent.IgniteAgent;
import io.github.dueris.eclipse.loader.api.GameLibrary;
import io.github.dueris.eclipse.loader.api.McVersion;
import io.github.dueris.eclipse.loader.game.GameProvider;
import io.github.dueris.eclipse.loader.game.GameTransformer;
import io.github.dueris.eclipse.loader.launch.EmberLauncher;
import io.github.dueris.eclipse.loader.util.BootstrapEntryContext;
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
	final MinecraftGameTransformer transformer = new MinecraftGameTransformer();
	private final String gameBrand;
	private final McVersion version;
	private final String mainClass;
	private final OptionSet optionSet;
	private final PaperclipJar paperclipJar;

	public MinecraftGameProvider() {
		Map<String, Object> properties = EmberLauncher.getProperties();
		Path gameJarPath = (Path) properties.get("gamejar");
		try {
			IgniteAgent.addJar(gameJarPath);
		} catch (final IOException exception) {
			throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
		}

		try {
			PaperclipJar paperclipJar = new PaperclipJar(gameJarPath.toFile());
			BootstrapEntryContext context = EclipseLoaderBootstrap.instance().context;
			this.gameBrand = context.brand();
			this.version = paperclipJar.mcVer();
			this.mainClass = paperclipJar.getMainClass();
			this.optionSet = OptionSetUtils.Serializer.deserialize(context.optionSet());
			this.paperclipJar = paperclipJar;
		} catch (IOException e) {
			throw new RuntimeException("Unable to build paperclip jar!", e);
		}
	}

	public MinecraftGameProvider(String gameBrand, McVersion version, String mainClass, OptionSet optionSet, PaperclipJar paperclipJar) {
		this.gameBrand = gameBrand;
		this.version = version;
		this.mainClass = mainClass;
		this.optionSet = optionSet;
		this.paperclipJar = paperclipJar;
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

	@Override
	public void initialize(EmberLauncher launcher) {
		this.transformer.transformContext();
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

	@Override
	public void launch(ClassLoader loader) {
		try {
			final Path gameJar = (Path) EmberLauncher.getProperties().get("gamejar");
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
