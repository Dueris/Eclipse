import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.8" apply true
    id("xyz.jpenilla.run-paper") version "2.2.3"
    id("ignite.parent-conventions")
    id("ignite.launcher-conventions")
    id("ignite.publish-conventions")
}

val paperweightVersion: String = "1.21.4-R0.1-SNAPSHOT"

extra["mcMajorVer"] = "21"
extra["mcMinorVer"] = "3"
extra["pluginVer"] = "v2.0.0"

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
        maven("https://jitpack.io")
    }

    tasks {
        processResources {
            val props = mapOf(
                "mcVer" to mcVer,
                "pluginVer" to pluginVer,
                "fullVer" to "mc$mcVer-$pluginVer",
                "apiVer" to "1.$mcMajorVer",
                "supported" to listOf("1.21", "1.21.1", "1.21.3")
            )
            inputs.properties(props)
            filesMatching("paper-plugin.yml") {
                expand(props)
            }

            filteringCharset = Charsets.UTF_8.name()
        }
    }

    tasks.getByName("compileJava").dependsOn(":paperweightUserdevSetup")
}

dependencies {
    implementation(libs.tinylog.impl)

    implementation(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "org.ow2.asm")
    }

    compileOnly(libs.mixinExtras) {
        exclude(group = "org.apache.commons")
    }

    compileOnly(libs.accessWidener)
    implementation(libs.asm)
    implementation(libs.asm.analysis)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.util)

    implementation(libs.gson)
    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")

    implementation(project("injection"))
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
}

tasks {
    runServer {
        minecraftVersion(mcVer)
    }
    shadowJar {
        mergeServiceFiles()
    }
}

tasks.register("eclipseJar") {
    dependsOn(":console:build", "shadowJar")

    doLast {
        val consoleJar = rootDir.resolve("console/build/libs/console-v1.0.0.jar").absoluteFile

        if (!consoleJar.exists()) {
            throw GradleException("Console JAR not found at: $consoleJar")
        }

        val shadowJarTask = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get()
        val shadowJar = shadowJarTask.archiveFile.get().asFile

        val tempJar = file("${buildDir}/libs/temp-${shadowJar.name}")

        var entryAlreadyExists = false
        shadowJar.inputStream().use { shadowJarInput ->
            ZipInputStream(shadowJarInput).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (entry.name == "nested-libs/console-v1.0.0.jar") {
                        entryAlreadyExists = true
                        break
                    }
                    entry = zipInput.nextEntry
                }
            }
        }

        if (entryAlreadyExists) {
            println("Entry 'nested-libs/console-v1.0.0.jar' already exists in ${shadowJar.name}. Skipping addition.")
            return@doLast
        }

        shadowJar.inputStream().use { shadowJarInput ->
            tempJar.outputStream().use { tempJarOutput ->
                ZipInputStream(shadowJarInput).use { zipInput ->
                    ZipOutputStream(tempJarOutput).use { zipOutput ->
                        var entry = zipInput.nextEntry
                        while (entry != null) {
                            zipOutput.putNextEntry(entry)
                            zipInput.copyTo(zipOutput)
                            zipOutput.closeEntry()
                            entry = zipInput.nextEntry
                        }

                        zipOutput.putNextEntry(ZipEntry("nested-libs/console-v1.0.0.jar"))
                        consoleJar.inputStream().use { it.copyTo(zipOutput) }
                        zipOutput.closeEntry()
                    }
                }
            }
        }

        shadowJar.delete()
        tempJar.renameTo(shadowJar)

        println("Added ${consoleJar.name} as a nested entry in ${shadowJar.name}")
    }
}
