buildscript {
    dependencies {
        classpath(libs.java.cup)
    }
}

plugins {
    kotlin("jvm")

    // Lexing & Parsing
    alias(libs.plugins.jflex)
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":runtime"))

    // Kotlin
    implementation(kotlin("reflect"))

    // Data structures
    implementation(libs.kotlinx.collections.immutable.jvm)
    implementation(libs.kotlinx.bimap)

    // Graphs
    implementation(libs.jgrapht.core)
    implementation(libs.jgrapht.io)

    // Unicode support
    implementation(libs.icu4j)

    // Parsing
    implementation(libs.java.cup.runtime)

    // Code generation
    api(libs.kotlinpoet) {
        exclude(module = "kotlin-reflect")
    }

    // SMT solving
    implementation(libs.z3.turnkey)

    // Testing
    testImplementation(project(":test-utilities"))
}

/** Compilation */

jflex {
    encoding.set(Charsets.UTF_8.name())
}

val compileCup by tasks.registering(CompileCupTask::class)

sourceSets {
    main {
        java.srcDir(compileCup.map { it.outputDirectory })
    }
}

tasks.compileKotlin.configure {
    dependsOn(tasks.withType<org.xbib.gradle.plugin.JFlexTask>())
    dependsOn(tasks.withType<CompileCupTask>())
}

tasks.withType<Test>().configureEach {
    systemProperties(
        "junit.jupiter.execution.parallel.enabled" to "true",
        "junit.jupiter.execution.parallel.mode.default" to "concurrent",
    )
}

// TODO: we only need to add explicit dependencies for dokkaHtmlPartial; dokkaHtml just works for some reason.
//   remove if/when Dokka fixes this issue.
tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dependsOn(compileCup)
    dependsOn(tasks.withType<org.xbib.gradle.plugin.JFlexTask>())
}

abstract class CompileCupTask : DefaultTask() {
    @InputDirectory
    @SkipWhenEmpty
    val sourceDirectory: DirectoryProperty =
        project.objects.directoryProperty().apply {
            convention(project.layout.projectDirectory.dir("src/main/cup"))
        }

    @OutputDirectory
    val outputDirectory: DirectoryProperty =
        project.objects.directoryProperty().apply {
            convention(project.layout.buildDirectory.dir("generated/sources/cup"))
        }

    @Input
    val cupArguments: ListProperty<String> =
        project.objects.listProperty<String>().apply {
            convention(listOf("-interface"))
        }

    @Internal
    override fun getGroup(): String = LifecycleBasePlugin.BUILD_TASK_NAME

    @Internal
    override fun getDescription(): String = "Generates Java sources from CUP grammar files."

    @TaskAction
    fun compileAll() {
        val cupFiles = project.fileTree(sourceDirectory) { include("**/*.cup") }
        if (cupFiles.filter { !it.isDirectory }.isEmpty) {
            logger.warn("no cup files found")
        }
        project.delete(outputDirectory)
        cupFiles.visit {
            if (!this.isDirectory) {
                compileFile(this.file)
            }
        }
    }

    private fun compileFile(cupFile: File) {
        val packagePath = cupFile.parentFile.relativeTo(sourceDirectory.get().asFile)
        val targetDirectory = outputDirectory.dir(packagePath.path)
        val packageName: String = packagePath.path.replace(File.separator, ".")
        val className: String = cupFile.nameWithoutExtension

        project.mkdir(targetDirectory)
        val args: List<String> =
            cupArguments.get() +
                listOf(
                    "-destdir",
                    targetDirectory.get().asFile.path,
                    "-package",
                    packageName,
                    "-parser",
                    className,
                    cupFile.path,
                )
        logger.info("java_cup ${args.joinToString(" ")}}")
        java_cup.Main.main(args.toTypedArray())
    }
}
