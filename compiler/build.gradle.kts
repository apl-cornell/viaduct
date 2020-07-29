buildscript {
    dependencies {
        classpath("com.github.vbmacher:java-cup:11b-20160615")
    }
}

plugins {
    application
    kotlin("jvm")

    // Bug finding
    jacoco

    // Lexing & Parsing
    id("org.xbib.gradle.plugin.jflex") version "1.2.0"
}

/** Application */

application {
    mainClass.set("${project.group}.${rootProject.name}.MainKt")
}

/** Dependencies */

dependencies {
    // Standard libraries
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

    // Functional data structures
    implementation("io.vavr:vavr:1.0.0-alpha-3")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")

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
    implementation(files("../deps/com.microsoft.z3.jar"))

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
    dependsOn(tasks.test)
}

// Enable assertions during manual testing
tasks.named<JavaExec>("run") {
    enableAssertions = true
}
