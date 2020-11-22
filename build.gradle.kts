import org.gradle.api.JavaVersion.VERSION_11

plugins {
    kotlin("multiplatform") version "1.4.20" apply false
    id("org.jetbrains.dokka") version "1.4.10.2"

    // Style checking
    id("com.diffplug.spotless") version "5.8.2"

    // Dependency management
    id("com.github.ben-manes.versions") version "0.36.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.15"
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")

    group = "edu.cornell.cs.apl"

    version = "0.1"

    repositories {
        jcenter()
        maven {
            url = uri("https://jitpack.io")
        }
    }

    /** Java Version */

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension>("java") {
            sourceCompatibility = VERSION_11
            targetCompatibility = VERSION_11
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }

    /** Style */

    spotless {
        java {
            target("src/**/*.java")
            googleJavaFormat()
        }
        kotlinGradle {
            ktlint()
        }
    }

    pluginManager.withPlugin("kotlin") {
        spotless {
            kotlin {
                val relativeBuildPath = project.buildDir.relativeTo(project.projectDir)
                targetExclude("$relativeBuildPath/**/*.kt")
                ktlint()
            }
        }
    }
}

/** Documentation */

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
        dokkaSourceSets {
            configureEach {
                includes.from("Module.md")
            }
        }
    }
}
