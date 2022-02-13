plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
    application

    // Style checking
    id("com.diffplug.spotless") version "6.2.0"
}

group = "edu.cornell.cs.apl"

val mainPackage = "${project.group}.${gradle.includedBuild("viaduct").name}.${project.name}"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    // Viaduct
    implementation("edu.cornell.cs.apl:runtime")

    // Command-line-argument parsing
    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")

    // Testing
    testImplementation("edu.cornell.cs.apl:test-utilities")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Getting a free port
    testImplementation("org.springframework:spring-core:5.3.15")
}

application {
    mainClass.set("$mainPackage.ExampleRunnerKt")
}

tasks.withType<Test> {
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
        val programs = source.walk().filter { it.isFile }.map {
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
