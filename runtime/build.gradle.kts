plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-native-mt")

    // Cryptography
    implementation("com.github.apl-cornell:aby-java:9f626e2b70")
    implementation(files("libs/jsnark.jar"))

    // Testing
    testImplementation(project(":test-utilities"))
}
