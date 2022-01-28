plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

/** Dependencies */

dependencies {
    api(project(":shared"))

    // Networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.1")

    // Cryptography
    api("com.github.apl-cornell:aby-java:0.1.1")

    // Testing
    testImplementation(project(":test-utilities"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}
