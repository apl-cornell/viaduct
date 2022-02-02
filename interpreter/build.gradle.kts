plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Cryptography
    implementation("com.github.apl-cornell:aby-java:0.1.1")
    implementation(files("libs/jsnark.jar"))

    // Testing
    testImplementation(project(":test-utilities"))
}
