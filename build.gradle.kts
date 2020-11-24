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

/** Kotlin Conventions */
// TODO: move into buildSrc when this is fixed: https://youtrack.jetbrains.com/issue/KT-41142
subprojects {
    pluginManager.withPlugin("kotlin") {
        apply(plugin = "jacoco")
        apply(plugin = "org.jetbrains.dokka")

        /** Java Version */

        extensions.configure<JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.allWarningsAsErrors = true
        }

        /** Dependencies */

        dependencies {
            // Data structures
            "implementation"("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.3")

            // Logging
            "implementation"("io.github.microutils:kotlin-logging:2.0.3")
            "testImplementation"("org.apache.logging.log4j:log4j-core:2.14.0")
            "testImplementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.14.0")

            // Testing
            "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.0")
            "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.7.0")
            "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.0")
        }

        /** Testing */

        tasks.named<Test>("test") {
            useJUnitPlatform()

            // Rerun tests when code examples change.
            inputs.files(project.fileTree("tests"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.isEnabled = true
                html.isEnabled = true
            }
            dependsOn(tasks["test"])
        }

        /** Documentation */

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includes.from("Module.md")
                }
            }
        }
    }
}
