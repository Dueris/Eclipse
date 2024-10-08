plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.build.nexus)
    implementation(libs.build.shadow)
}

dependencies {
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
}
