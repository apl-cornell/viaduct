plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
    application

    // Style checking
    id("com.diffplug.spotless") version "6.2.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    // Viaduct
    implementation("edu.cornell.cs.apl:runtime")

    // Reflection
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")

    // Command-line-argument parsing
    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")

    testImplementation("edu.cornell.cs.apl:test-utilities")
}

application {
    mainClass.set("edu.cornell.cs.apl.viaduct.codegeneration.CodegenRunnerKt")
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
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateViaductProgramList.map { it.outputDirectory })
}

abstract class GenerateViaductProgramList : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

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

        outputDirectory.get().asFile.mkdirs()
        val outputFile = outputDirectory.file("ViaductPrograms.kt").get().asFile

        val programsBlock = programs.map { "    $it" }.joinToString(",\n")
        outputFile.writeText("val viaductPrograms = listOf(\n$programsBlock\n)\n")
    }
}
