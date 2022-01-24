plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")

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
    implementation("edu.cornell.cs.apl:runtime")

    testImplementation("edu.cornell.cs.apl:test-utilities")
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
