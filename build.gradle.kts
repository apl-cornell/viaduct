buildscript {
    dependencies {
        classpath("com.github.vbmacher:java-cup:11b-20160615")
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.dokka") version "0.10.1"

    // Style checking
    id("com.diffplug.gradle.spotless") version "3.27.1"
    id("org.ec4j.editorconfig") version "0.0.3"

    // Bug finding
    jacoco

    // Lexing & Parsing
    id("org.xbib.gradle.plugin.jflex") version "1.2.0"
}

group = "edu.cornell.cs.apl"

version = "0.1"

repositories {
    jcenter()
}


/** Application */

application {
    mainClassName = "${project.group}.${project.name}.MainKt"
}

tasks.jar {
    manifest {
        attributes(Pair("Main-Class", application.mainClassName))
    }
}


/** Dependencies */

dependencies {
    // Standard libraries
    implementation(kotlin("stdlib-jdk8"))

    // Functional data structures
    implementation("io.vavr:vavr:0.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")

    // Google's Guava (core data structures)
    implementation("com.google.guava:guava:28.1-jre")

    // Google's AutoValue for creating immutable classes
    implementation("com.google.auto.value:auto-value-annotations:1.6.2")
    annotationProcessor("com.google.auto.value:auto-value:1.6.2")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.3.1")
    implementation("org.jgrapht:jgrapht-io:1.3.1")

    // Unicode support
    implementation("com.ibm.icu:icu4j:64.2")

    // Command-line-argument parsing
    implementation("com.github.ajalt:clikt:2.5.0")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:1.18")

    // Parsing
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.11.0")

    // Logging (disabled for now using the NOP engine)
    implementation("org.slf4j:slf4j-nop:1.8.0-beta4")

    // Testing
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}


/** Compilation */

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.allWarningsAsErrors = true
}

val compileCup by tasks.registering(CupCompileTask::class) {}

sourceSets {
    main {
        java.srcDir(compileCup.get().generateDir)
    }
}

tasks.compileJava {
    dependsOn(compileCup)
}

tasks.compileKotlin {
    dependsOn(compileCup)
    dependsOn(tasks.jflex)
}

open class CupCompileTask : DefaultTask() {
    @InputDirectory
    val sourceDir: File = project.file("src/main/cup")

    @OutputDirectory
    val generateDir: File = project.file("${project.buildDir}/generated-src/cup")

    @Input
    val cupArguments: List<String> = listOf("-interface")

    @Input
    override fun getDescription(): String {
        return "Generates Java sources from CUP grammar files."
    }

    @TaskAction
    fun compileAll() {
        val cupFiles = project.fileTree(sourceDir) { include("**/*.cup") }
        if (cupFiles.filter { !it.isDirectory }.isEmpty) {
            logger.warn("no cup files found")
        }
        project.delete(generateDir)
        cupFiles.visit {
            if (!this.isDirectory) {
                compileFile(this.file)
            }
        }
    }

    private fun compileFile(cupFile: File) {
        val packagePath = cupFile.parentFile.relativeTo(sourceDir).toPath()
        val outputDirectory = generateDir.toPath().resolve(packagePath).toAbsolutePath()
        val packageName: String = packagePath.toString().replace(File.separator, ".")
        val className: String = cupFile.nameWithoutExtension

        project.mkdir(outputDirectory)
        val args: List<String> = cupArguments +
            listOf(
                "-destdir", outputDirectory.toString(),
                "-package", packageName,
                "-parser", className,
                cupFile.absolutePath
            )
        java_cup.Main.main(args.toTypedArray())
    }
}


/** Checks */

// TODO: remove once Java is gone
spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
    }
    kotlin {
        ktlint()
    }
}

editorconfig {
    excludes = listOf("$buildDir", "out", ".kotlin", "**/*.hprof")
}

tasks.check {
    dependsOn(tasks.editorconfigCheck)
}


/** Testing */

tasks.test {
    useJUnitPlatform()

    // Rerun tests when code examples change.
    inputs.files(project.fileTree("examples"))
    inputs.files(project.fileTree("errors"))
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

// Enable assertions during manual testing
tasks.named<JavaExec>("run") {
    enableAssertions = true
}


/** Documentation */

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/docs/html"
}

val dokkaGitHub by tasks.registering(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputFormat = "gfm"
    outputDirectory = "$buildDir/docs/markdown"
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    subProjects = subprojects.map { it.name }
    configuration {
        includes = listOf("packages.md")
    }
}
