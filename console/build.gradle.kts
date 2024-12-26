version = "v1.0.0"

println("Loaded subproject \"${project.name}\" with version '$version'")

dependencies {
    compileOnly(libs.tinylog.impl)
}

tasks.getByName("compileJava").dependsOn(":injection:paperweightUserdevSetup")