import io.github.dueris.kotlin.eclipse.gradle.MinecraftVersion

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1" apply true
    id("io.github.dueris.eclipse.gradle") version "1.2.1" apply true
}

val paperweightVersion: String = "1.21.3-R0.1-SNAPSHOT"

apply(plugin = "java")
apply(plugin = "maven-publish")
apply(plugin = "io.papermc.paperweight.userdev")
apply(plugin = "io.github.dueris.eclipse.gradle")
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    paperweight.paperDevBundle(paperweightVersion)
    implementation(rootProject)
}

eclipse {
    minecraft = MinecraftVersion.MC1_21_3
    wideners = files("example.accesswidener")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.inventivetalent.org/repository/public/")
    maven("https://repo.codemc.org/repository/maven-releases/")
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://libraries.minecraft.net/")
    maven("https://jitpack.io")
}