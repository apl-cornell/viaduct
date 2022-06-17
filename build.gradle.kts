plugins {
    kotlin("multiplatform") version "1.7.0" apply false
    kotlin("plugin.serialization") version "1.6.21" apply false

    // Versioning
    id("com.palantir.git-version") version "0.15.0"

    // Documentation
    id("org.jetbrains.dokka") version "1.6.21"

    // Style checking
    id("com.diffplug.spotless") version "6.7.2"
}

// Derive version from Git tags
val gitVersion: groovy.lang.Closure<String> by extra
val versionFromGit = gitVersion()

allprojects {
    apply(plugin = "com.diffplug.spotless")

    group = "io.github.apl-cornell.${rootProject.name}"

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
            // Logging
            "implementation"("io.github.microutils:kotlin-logging:2.1.23")
            "testImplementation"("org.apache.logging.log4j:log4j-core:2.17.2")
            "testImplementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
        }

        /** Testing */

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()

            // Rerun tests when code examples change.
            inputs.files(project.fileTree("tests"))
        }

        tasks.withType<JacocoReport>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
            dependsOn(tasks.withType<Test>())
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
