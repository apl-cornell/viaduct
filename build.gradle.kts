plugins {
    kotlin("multiplatform") version embeddedKotlinVersion apply false
    kotlin("plugin.serialization") version embeddedKotlinVersion apply false

    // Versioning
    id("com.palantir.git-version") version "0.12.3"

    // Documentation
    id("org.jetbrains.dokka") version "1.6.0"

    // Style checking
    id("com.diffplug.spotless") version "6.0.5"

    // Dependency management
    id("com.github.ben-manes.versions") version "0.39.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

// Derive version from Git tags
val gitVersion: groovy.lang.Closure<String> by extra
val versionFromGit = gitVersion()

allprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")

    group = "edu.cornell.cs.apl"

    version = if (versionFromGit == "unspecified") "0.0.0-SNAPSHOT" else versionFromGit

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

/** Kotlin Conventions */
// TODO: move into buildSrc when this is fixed: https://youtrack.jetbrains.com/issue/KT-41142
subprojects {
    pluginManager.withPlugin("kotlin") {
        apply(plugin = "jacoco")
        apply(plugin = "org.jetbrains.dokka")

        /** Java Version */

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.allWarningsAsErrors = true
        }

        /** Dependencies */

        dependencies {
            // Data structures
            "implementation"("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")

            // Logging
            "implementation"("io.github.microutils:kotlin-logging:2.1.0")
            "testImplementation"("org.apache.logging.log4j:log4j-core:2.17.0")
            "testImplementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
        }

        /** Testing */

        tasks.named<Test>("test") {
            useJUnitPlatform()

            // Rerun tests when code examples change.
            inputs.files(project.fileTree("tests"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
            dependsOn(tasks["test"])
        }

        /** API Documentation */

        tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includes.from("Module.md")
                }
            }
        }
    }
}
