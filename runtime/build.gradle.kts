plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    // Cryptography
    implementation("de.tu_darmstadt.cs.encrypto:aby-java:f96aceac2c7096499c26d91cad1e560e069f6aa2")
    implementation(files("libs/jsnark.jar"))

    // Testing
    testImplementation(project(":test-utilities"))
}
