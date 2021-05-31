plugins {
    kotlin("multiplatform") version "1.5.0" apply false

    // Documentation
    id("org.jetbrains.dokka") version "1.4.20"
    id("ru.vyarus.mkdocs") version "2.1.1"

    // Style checking
    id("com.diffplug.spotless") version "5.12.5"

    // Dependency management
    id("com.github.ben-manes.versions") version "0.39.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.16"
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")

    group = "edu.cornell.cs.apl"

    version = "0.1"

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

        // TODO: remove once Gradle updates to the newer version
        extensions.configure<JacocoPluginExtension>("jacoco") {
            toolVersion = "0.8.7"
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.allWarningsAsErrors = true
        }

        /** Dependencies */

        dependencies {
            // Data structures
            "implementation"("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")

            // Logging
            "implementation"("io.github.microutils:kotlin-logging:2.0.6")
            "testImplementation"("org.apache.logging.log4j:log4j-core:2.14.1")
            "testImplementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
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

        /** API Documentation */

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includes.from("Module.md")
                }
            }
        }
    }
}

/** Documentation */

mkdocs {
    sourcesDir = "docs"
    buildDir = "${project.buildDir}/mkdocs"

    publish.apply {
        val projectVersion = "${project.version}"
        if (System.getenv("GITHUB_REF") == "refs/heads/master") {
            // Publishing to master; update latest version pointer
            docPath = projectVersion
            rootRedirect = true
        } else {
            // Publishing some other commit; don't update latest version pointer
            docPath = System.getenv("GITHUB_SHA") ?: projectVersion
            rootRedirect = false
        }
    }
}

tasks.withType<ru.vyarus.gradle.plugin.mkdocs.task.MkdocsTask>().configureEach {
    dependsOn(tasks.dokkaGfmMultiModule)
}

python {
    // virtualenv fails without this setting.
    envCopy = true

    // Update library versions
    pip("mkdocs:1.1.2")
    pip("mkdocs-material:7.0.3")
    pip("pygments:2.8.0")
    pip("pymdown-extensions:8.1.1")
}
