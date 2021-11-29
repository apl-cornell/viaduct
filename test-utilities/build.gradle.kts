plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api("org.junit.jupiter:junit-jupiter-api:5.8.2")
    api("org.junit.jupiter:junit-jupiter-params")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
