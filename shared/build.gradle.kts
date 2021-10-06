plugins {
    kotlin("jvm")
}

val mainPackage = "${project.group}.${rootProject.name}"

/** Dependencies */

dependencies {
    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.3.4")

    // Testing
    testImplementation(project(":test-utilities"))
}

/** Compilation */

val generatedPropertiesDir = "${project.buildDir}/generated/sources/properties"

val generatePropertiesFile by tasks.registering {
    doLast {
        val packageDir = mainPackage.replace(".", File.separator)
        val propertiesFile = project.file("$generatedPropertiesDir/$packageDir/Properties.kt")
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
    kotlin.srcDir(generatedPropertiesDir)
}

tasks.compileKotlin {
    dependsOn(generatePropertiesFile)
}
