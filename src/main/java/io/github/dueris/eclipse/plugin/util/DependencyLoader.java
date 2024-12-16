package io.github.dueris.eclipse.plugin.util;

import io.github.dueris.eclipse.loader.Main;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class DependencyLoader implements PluginLoader {

	@Override
	public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
		if (Main.BOOTED.get()) {
			return;
		}
		MavenLibraryResolver resolver = new MavenLibraryResolver();

		maven(resolver, "https://repo.papermc.io/repository/maven-public/");
		maven(resolver, "https://oss.sonatype.org/content/groups/public/");
		maven(resolver, "https://repo.opencollab.dev/main/");
		maven(resolver, "https://repo.extendedclip.com/content/repositories/placeholderapi/");
		maven(resolver, "https://repo.inventivetalent.org/repository/public/");
		maven(resolver, "https://repo.codemc.org/repository/maven-releases/");
		maven(resolver, "https://maven.quiltmc.org/repository/release/");
		maven(resolver, "https://maven.fabricmc.net/");

		for (String o : List.of("org.ow2.asm:asm:9.7", "org.ow2.asm:asm-util:9.7", "com.github.olivergondza:maven-jdk-tools-wrapper:0.1",
			"org.javassist:javassist:3.30.2-GA", "net.bytebuddy:byte-buddy-agent:1.14.19", "io.github.kasukusakura:jvm-self-attach:0.0.1",
			"io.github.karlatemp:unsafe-accessor:1.7.0", "org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")) {
			resolver.addDependency(new Dependency(new DefaultArtifact(o), null));
		}
		classpathBuilder.addLibrary(resolver);
	}

	private void maven(@NotNull MavenLibraryResolver resolver, String url) {
		resolver.addRepository(new RemoteRepository.Builder(url.replace("https://", "")
															   .split("/")[0], "default", url).build());
	}
}
