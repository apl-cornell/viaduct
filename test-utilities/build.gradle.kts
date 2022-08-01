plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api("org.junit.jupiter:junit-jupiter-api:5.9.0")
    api("org.junit.jupiter:junit-jupiter-params")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
