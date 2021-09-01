plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))

    // Cryptography
    implementation("de.tu_darmstadt.cs.encrypto:aby-java:f96aceac2c7096499c26d91cad1e560e069f6aa2")

    // Testing
    testImplementation(project(":test-utilities"))
}
