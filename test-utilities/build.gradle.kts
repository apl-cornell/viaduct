plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform)
}
