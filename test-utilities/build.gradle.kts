plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.jupiter.engine)
}
