plugins {
    kotlin("jvm")
}

val rootPackage: String by project.ext

/** Dependencies */

dependencies {
    // Data structures
    implementation(libs.kotlinx.collections.immutable.jvm)

    // Colored terminal output
    implementation(libs.jansi)

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
