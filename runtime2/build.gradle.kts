plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

/** Dependencies */

dependencies {
    api(project(":shared"))

    // concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.1")

    // Cryptography

    // Testing
    testImplementation(project(":test-utilities"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}
