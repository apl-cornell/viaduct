plugins {
    kotlin("multiplatform") version "1.3.71" apply false

    // Style checking
    id("com.diffplug.gradle.spotless") version "4.3.0"
    id("org.ec4j.editorconfig") version "0.0.3"

    // Dependency management
    id("com.github.ben-manes.versions") version "0.28.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
}

allprojects {
    apply(plugin = "com.diffplug.gradle.spotless")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")

    group = "edu.cornell.cs.apl"

    version = "0.1"

    repositories {
        jcenter()
    }

    /** Style */

    spotless {
        kotlinGradle {
            ktlint()
        }
    }
}

/** Style */

project(":compiler") {
    spotless {
        // TODO: remove once Java is gone
        java {
            target("src/**/*.java")
            googleJavaFormat()
        }
        kotlin {
            ktlint()
        }
    }
}

editorconfig {
    excludes = listOf("$buildDir", "out", "gradlew", ".kotlin", "**/*.hprof")
}

tasks.check {
    dependsOn(tasks.editorconfigCheck)
}
