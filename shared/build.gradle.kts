plugins {
    kotlin("jvm")
}

val mainPackage = project.group as String

/** Dependencies */

dependencies {
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
        val packageDir = mainPackage.replace(".", File.separator)
        val propertiesFile = outputDir.get().dir(packageDir).file("Properties.kt").asFile
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

kotlin.sourceSets.main {
    kotlin.srcDir(generatePropertiesFile.map { it.outputs })
}
