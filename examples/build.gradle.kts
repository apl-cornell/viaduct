plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.apl-cornell.viaduct")
    application

    // Style checking
    alias(libs.plugins.spotless)
}

group = "io.github.apl-cornell.viaduct"

val mainPackage = "${(project.group as String).replace("-", "")}.${project.name}"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Viaduct
    implementation("${project.group}:runtime")

    // Command-line-argument parsing
    implementation(libs.clikt)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j2.impl)

    // Testing
    testImplementation("${project.group}:test-utilities")
    testImplementation(libs.kotlinx.coroutines.core)
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
    sourceDirectory.set(tasks.compileViaductMain.map { it.sourceDirectory }.get())
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
