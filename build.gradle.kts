plugins {
    kotlin("multiplatform") version "1.3.71" apply false
    id("org.jetbrains.dokka") version "0.10.1"

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
    // Google's Guava (core data structures)
    implementation("com.google.guava:guava:29.0-jre")

    // Google's AutoValue for creating immutable classes
    implementation("com.google.auto.value:auto-value-annotations:1.7.3")
    annotationProcessor("com.google.auto.value:auto-value:1.7.3")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.4.0")
    implementation("org.jgrapht:jgrapht-io:1.4.0")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.16.3")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.16.3")

    // Unicode support
    implementation("com.ibm.icu:icu4j:67.1")

    // Command-line-argument parsing
    implementation("com.github.ajalt:clikt:2.5.0")

    // bimap
    implementation("com.uchuhimo:kotlinx-bimap:1.2")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:1.18")

    // Parsing
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.7.10")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")

    // Z3
    implementation(files("deps/com.microsoft.z3.jar"))

    // Testing
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0-M1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0-M1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0-M1")
}

/** Compilation */

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.allWarningsAsErrors = true
}

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

/** Documentation */

val dokkaHtml by tasks.registering(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/docs/$outputFormat"
}

val dokkaGfm by tasks.registering(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputFormat = "gfm"
    outputDirectory = "$buildDir/docs/$outputFormat"
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    // TODO: we will be able to remove all this with the next release of Dokka
    subProjects = subprojects.map { it.name }
    configuration {
        includes = subprojects.map { it.file("packages.md") }.filter { it.exists() }.map { it.toString() }
    }
}
