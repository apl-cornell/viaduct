buildscript {
    dependencies {
        classpath("com.github.vbmacher:java-cup:11b-20160615-1")
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

    // Data structures
    implementation("com.uchuhimo:kotlinx-bimap:1.2")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation("org.jgrapht:jgrapht-io:1.5.1")

    // Unicode support
    implementation("com.ibm.icu:icu4j:69.1")

    // Parsing
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615-1")

    // SMT solving
    implementation("io.github.tudo-aqua:z3-turnkey:4.8.7.1")

    // Testing
    testImplementation(project(":test-utilities"))
    testImplementation(kotlin("reflect"))
}

/** Compilation */

jflex {
    encoding = Charsets.UTF_8.name()
}

val compileCup by tasks.registering(CompileCupTask::class) {}

sourceSets {
    main {
        java.srcDir(compileCup.get().outputDirectory)
    }
}

tasks.compileJava {
    dependsOn(compileCup)
}

tasks.compileKotlin {
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
    override fun getGroup(): String =
        LifecycleBasePlugin.BUILD_TASK_NAME

    @Internal
    override fun getDescription(): String =
        "Generates Java sources from CUP grammar files."

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
        val args: List<String> = cupArguments.get() +
            listOf(
                "-destdir", targetDirectory.get().asFile.path,
                "-package", packageName,
                "-parser", className,
                cupFile.path
            )
        logger.info("java_cup ${args.joinToString(" ")}}")
        java_cup.Main.main(args.toTypedArray())
    }
}
