plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Data structures
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.5")

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Cryptography
    implementation("io.github.apl-cornell:aby-java:0.2.2")

    // Testing
    testImplementation(project(":test-utilities"))
}
