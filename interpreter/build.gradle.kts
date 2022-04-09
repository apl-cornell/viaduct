plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Data structures
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    // Cryptography
    implementation("io.github.apl-cornell:aby-java:0.2.2")
    implementation(files("libs/jsnark.jar"))

    // Testing
    testImplementation(project(":test-utilities"))
}
