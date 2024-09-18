"eclipse".also { rootProject.name = it }

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

sequenceOf(
    "api",
    "launcher"
).forEach {
    include("ignite-$it")
    project(":ignite-$it").projectDir = file(it)
}
