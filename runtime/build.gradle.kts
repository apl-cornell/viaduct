plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

/** Dependencies */

dependencies {
    api(project(":shared"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    // Networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.3")

    // Cryptography
    api("io.github.apl-cornell:aby-java:0.2.2")

    // Testing
    testImplementation(project(":test-utilities"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}
