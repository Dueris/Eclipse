import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("ignite.base-conventions")
    id("com.gradleup.shadow")
}

// Expose version catalog
val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

val implementationVersion = project.version.toString()
val regexPattern = """(\d+\.\d+)""".toRegex()
val apiVersion = regexPattern.find(implementationVersion)?.value

tasks.getByName<Jar>("jar") {
    manifest {
        attributes(
            "Premain-Class" to "io.github.dueris.eclipse.loader.MixinJavaAgent",
            "Agent-Class" to "io.github.dueris.eclipse.loader.MixinJavaAgent",
            "Launcher-Agent-Class" to "io.github.dueris.eclipse.loader.MixinJavaAgent",
            "Main-Class" to "io.github.dueris.eclipse.loader.Main",
            "Multi-Release" to true,
            "Automatic-Module-Name" to "net.minecrell.terminalconsole",

            "Specification-Title" to "eclipse",
            "Specification-Version" to apiVersion,
            "Specification-Vendor" to "Dueris",

            "Implementation-Title" to project.name,
            "Implementation-Version" to implementationVersion,
            "Implementation-Vendor" to "Dueris"
        )

        attributes(
            "org/objectweb/asm/",
            "Implementation-Version" to "9.7.1"
        )
    }
}

tasks.getByName<ShadowJar>("shadowJar") {
    mergeServiceFiles()

    relocate("com.google.gson", "io.github.dueris.eclipse.loader.libs.gson")
}
