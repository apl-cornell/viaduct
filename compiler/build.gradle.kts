buildscript {
    dependencies {
        classpath("com.github.vbmacher:java-cup:11b-20160615")
    }
}

plugins {
    kotlin("jvm")

    // Lexing & Parsing
    id("org.xbib.gradle.plugin.jflex") version "1.5.0"
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")

    // Data structures
    implementation("com.uchuhimo:kotlinx-bimap:1.2")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.5.0")
    implementation("org.jgrapht:jgrapht-io:1.5.0")

    // Unicode support
    implementation("com.ibm.icu:icu4j:68.1")

    // Parsing
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")

    // SMT solving
    implementation("io.github.tudo-aqua:z3-turnkey:4.8.7.1")

    // Cryptography
    implementation("com.github.apl-cornell:aby-java:9f626e2b70")
    implementation(files("libs/jsnark.jar"))

    // Testing
    testImplementation(kotlin("reflect"))
}

/** Compilation */

jflex {
    encoding = Charsets.UTF_8.name()
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
    override fun getGroup(): String =
        JavaBasePlugin.BUILD_TASK_NAME

    @Input
    override fun getDescription(): String =
        "Generates Java sources from CUP grammar files."

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
