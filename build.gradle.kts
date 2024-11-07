plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // Versioning
    alias(libs.plugins.git.version)

    // Documentation
    alias(libs.plugins.dokka)

    // Style checking
    alias(libs.plugins.spotless)
}

// Derive version from Git tags
val gitVersion: groovy.lang.Closure<String> by extra
val versionFromGit = gitVersion()

allprojects {
    apply(plugin = "com.diffplug.spotless")

    group = "io.github.apl-cornell.${rootProject.name}"

    version = if (versionFromGit == "unspecified") "0.0.0-SNAPSHOT" else versionFromGit

    ext.set("rootPackage", (group as String).replace("-", ""))

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
                val relativeBuildPath = project.layout.buildDirectory.asFile.get().relativeTo(project.projectDir)
                targetExclude("$relativeBuildPath/**/*.kt")
                ktlint()
            }
        }
    }
}

/** Kotlin Conventions */
subprojects {
    // TODO: move into buildSrc when this is fixed: https://youtrack.jetbrains.com/issue/KT-41142
    pluginManager.withPlugin("kotlin") {
        apply(plugin = "jacoco")
        apply(plugin = "org.jetbrains.dokka")

        /** Java Version */

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.allWarningsAsErrors = true
        }

        /** Dependencies */

        dependencies {
            // Logging
            "implementation"(libs.kotlin.logging)
            "testImplementation"(platform(libs.log4j.bom))
            "testImplementation"(libs.log4j.core)
            "testImplementation"(libs.log4j.slf4j2.impl)
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
