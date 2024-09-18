plugins {
    id("ignite.base-conventions")
    `maven-publish`
    signing
}

// Expose version catalog
val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

java {
    withJavadocJar()
}
