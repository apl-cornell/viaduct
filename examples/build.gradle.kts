plugins {
    kotlin("jvm") version "1.7.10"
    id("io.github.apl-cornell.viaduct")
    application

    // Style checking
    id("com.diffplug.spotless") version "6.7.2"
}

group = "io.github.apl-cornell.viaduct"

val mainPackage = "${(project.group as String).replace('-', '_')}.${project.name}"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    // Viaduct
    implementation("${project.group}:runtime")

    // Command-line-argument parsing
    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.23")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")

    // Testing
    testImplementation("${project.group}:test-utilities")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
}

application {
    mainClass.set("$mainPackage.ExampleRunnerKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

spotless {
    kotlinGradle {
        ktlint()
    }
    kotlin {
        val relativeBuildPath = project.buildDir.relativeTo(project.projectDir)
        targetExclude("$relativeBuildPath/**/*.kt")
        ktlint()
    }
}

val generateViaductProgramList by tasks.registering(GenerateViaductProgramList::class) {
    sourceDirectory.set(tasks.compileViaduct.map { it.sourceDirectory }.get())
    outputPackage.set(mainPackage)
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateViaductProgramList.map { it.outputDirectory })
}

abstract class GenerateViaductProgramList : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:Input
    abstract val outputPackage: Property<String>

    @OutputDirectory
    val outputDirectory: DirectoryProperty =
        project.objects.directoryProperty().apply {
            convention(project.layout.buildDirectory.dir("generated/sources/properties"))
        }

    @Internal
    override fun getGroup(): String =
        LifecycleBasePlugin.BUILD_TASK_NAME

    @Internal
    override fun getDescription(): String =
        "Generates a list of all Viaduct compiled programs."

    @TaskAction
    fun generate() {
        val source = sourceDirectory.get().asFile
        val programs = source.walk()
            .filter { it.isFile }
            .filter { it.extension == "via" }
            .map {
                val packageName = it.parentFile.toRelativeString(source).replace(File.separator, ".")
                val className = it.nameWithoutExtension
                if (packageName.isEmpty()) className else "$packageName.$className"
            }.sorted()

        val packageDirectory = outputDirectory.get().asFile.resolve(outputPackage.get().replace(".", File.separator))
        val outputFile = packageDirectory.resolve("ViaductPrograms.kt")
        packageDirectory.mkdirs()

        val programsBlock = programs.map { "    $it" }.joinToString(",\n")
        val programsDeclaration = "val viaductPrograms = listOf(\n$programsBlock\n)\n"
        outputFile.writeText("package ${outputPackage.get()}\n\n$programsDeclaration")
    }
}
