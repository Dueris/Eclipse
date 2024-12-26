version = "v1.0.0"

println("Loaded subproject \"${project.name}\" with version '$version'")

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")

    compileOnly("org.ow2.asm:asm:9.7")
    compileOnly("org.ow2.asm:asm-util:9.7")
    compileOnly("com.github.olivergondza:maven-jdk-tools-wrapper:0.1")
    compileOnly("org.javassist:javassist:3.30.2-GA")
    compileOnly("net.bytebuddy:byte-buddy-agent:1.14.19")
    compileOnly("io.github.kasukusakura:jvm-self-attach:0.0.1")
    compileOnly("io.github.karlatemp:unsafe-accessor:1.7.0")
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
}

tasks.getByName("compileJava").dependsOn(":paperweightUserdevSetup")