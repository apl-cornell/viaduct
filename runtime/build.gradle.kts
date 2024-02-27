plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

/** Dependencies */

dependencies {
    api(project(":shared"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Networking
    implementation(libs.kotlinx.serialization.protobuf)

    // Cryptography
    api(libs.aby.java)

    // Testing
    testImplementation(project(":test-utilities"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}
