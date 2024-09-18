import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1" apply true
    id("xyz.jpenilla.run-paper") version "2.2.3"
    id("ignite.parent-conventions")
}

val paperweightVersion: String = "1.21-R0.1-SNAPSHOT"

extra["mcMajorVer"] = "21"
extra["mcMinorVer"] = "1"
extra["pluginVer"] = "v1.0.0"

val mcMajorVer = extra["mcMajorVer"] as String
val mcMinorVer = extra["mcMinorVer"] as String
val pluginVer = extra["pluginVer"] as String

val mcVer = "1.$mcMajorVer" + if (mcMinorVer == "0") "" else ".$mcMinorVer"
extra["mcVer"] = mcVer
extra["fullVer"] = "mc$mcVer-$pluginVer"

println("Loading plugin version: $pluginVer")
println("Loading minecraft version: $mcVer")

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "io.papermc.paperweight.userdev")
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
    }

    tasks {
        processResources {
            val props = mapOf(
                "mcVer" to mcVer,
                "pluginVer" to pluginVer,
                "fullVer" to "mc$mcVer-$pluginVer",
                "apiVer" to "1.$mcMajorVer",
                "supported" to listOf("1.21", "1.21.1")
            )
            inputs.properties(props)
            filesMatching("paper-plugin.yml") {
                expand(props)
            }

            filteringCharset = Charsets.UTF_8.name()
        }
    }

}

dependencies {
    implementation(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "org.ow2.asm")
    }
    implementation(libs.mixinExtras) {
        exclude(group = "org.apache.commons")
    }

    implementation("jline:jline:2.12.1")
}

tasks {
    runServer {
        minecraftVersion(mcVer)
    }
}

tasks.all {
    dependsOn(":ignite-launcher:shadowJar")

    doLast {
        moveIgniteJar()
    }
}

fun moveIgniteJar() {
    val sourceDir = "build/libs"
    val targetDir = "src/main/resources"
    val fileName = "ignite.jar"

    File(sourceDir).takeIf { it.exists() && it.isDirectory }?.let { srcDir ->
        File(targetDir).takeIf { it.exists() && it.isDirectory }?.let { tgtDir ->
            for (listFile in srcDir.listFiles()) {
                if (listFile.name.equals(fileName)) {
                    val sourceFile = listFile
                    val targetFile = File(tgtDir, fileName)

                    if (sourceFile.exists() && sourceFile.isFile) {
                        Files.copy(
                            rootDir.resolve(sourceFile.toPath().toString()).toPath(),
                            rootDir.resolve(targetFile.toPath().toString()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    } else {
                        println("Source file does not exist: '${sourceFile.path}'")
                    }
                }
            }
        } ?: println("Target directory does not exist or is not a directory: '$targetDir'")
    } ?: println("Source directory does not exist or is not a directory: '$sourceDir'")
}
