plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

/** Dependencies */

dependencies {
    api(project(":shared"))

    // Networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.0")

    // Cryptography

    // Testing
    testImplementation(project(":test-utilities"))
}

kotlin.sourceSets.all {
    languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
}
