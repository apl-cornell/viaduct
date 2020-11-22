// import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    id("org.xbib.gradle.plugin.jflex") version "1.5.0"
}

/** Application */

val mainPackage = "${project.group}.${rootProject.name}"

application {
    mainClass.set("$mainPackage.MainKt")
}

/** Dependencies */

dependencies {
    // Standard libraries
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")

    // Data structures
    implementation("io.vavr:vavr:1.0.0-alpha-3")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.3")
    implementation("com.uchuhimo:kotlinx-bimap:1.2")

    // Google's Guava (core data structures)
    implementation("com.google.guava:guava:30.0-jre")

    // Google's AutoValue for creating immutable classes
    implementation("com.google.auto.value:auto-value-annotations:1.7.4")
    annotationProcessor("com.google.auto.value:auto-value:1.7.4")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.5.0")
    implementation("org.jgrapht:jgrapht-io:1.5.0")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.18.0")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.0")

    // Unicode support
    implementation("com.ibm.icu:icu4j:68.1")

    // Command-line-argument parsing
    implementation("com.github.ajalt:clikt:2.8.0")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.0.1")

    // Parsing
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")

    // SMT solving
    implementation("io.github.tudo-aqua:z3-turnkey:4.8.7.1")

    // Cryptography
    implementation("com.github.apl-cornell:aby-java:f061249362")

    implementation(files("libs/jsnark.jar"))

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.0")

    // Testing
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

/** Compilation */

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
}

val generatedPropertiesDir = "${project.buildDir}/generated-src/properties"

val generatePropertiesFile by tasks.registering {
    doLast {
        val packageDir = mainPackage.replace(".", File.separator)
        val propertiesFile = project.file("$generatedPropertiesDir/$packageDir/Properties.kt")
        propertiesFile.parentFile.mkdirs()
        propertiesFile.writeText(
            """
            package $mainPackage

            const val version = "${project.version}"

            const val group = "${project.group}"
            """.trimIndent()
        )
    }
}

jflex {
    encoding = Charsets.UTF_8.name()
}

val compileCup by tasks.registering(CupCompileTask::class) {}

sourceSets {
    main {
        java.srcDir(compileCup.get().generateDir)
    }
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir(generatedPropertiesDir)
    }
}

tasks.compileJava {
    dependsOn(compileCup)
}

tasks.compileKotlin {
    dependsOn(generatePropertiesFile)
    dependsOn(compileCup)
    dependsOn(tasks.withType<org.xbib.gradle.plugin.JFlexTask>())
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
