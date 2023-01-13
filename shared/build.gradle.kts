plugins {
    kotlin("jvm")
}

val rootPackage: String by project.ext

/** Dependencies */

dependencies {
    // Data structures
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.5")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.4.0")

    // Testing
    testImplementation(project(":test-utilities"))
}

/** Compilation */

val generatePropertiesFile by tasks.registering {
    val outputDir = project.layout.buildDirectory.dir("generated/sources/properties")
    outputs.dir(outputDir)

    doLast {
        val packageDir = rootPackage.replace(".", File.separator)
        val propertiesFile = outputDir.get().dir(packageDir).file("Properties.kt").asFile
        propertiesFile.parentFile.mkdirs()
        propertiesFile.writeText(
            """
            package $rootPackage

            const val version = "${project.version}"

            const val group = "${project.group}"
            """.trimIndent(),
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generatePropertiesFile.map { it.outputs })
}
