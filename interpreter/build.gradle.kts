plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Data structures
    implementation(libs.kotlinx.collections.immutable.jvm)

    // Concurrency
    implementation(libs.kotlinx.coroutines.core)

    // Cryptography
    implementation(libs.aby.java)

    // Testing
    testImplementation(project(":test-utilities"))
}
