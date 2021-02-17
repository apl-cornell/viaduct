plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api("org.junit.jupiter:junit-jupiter-api:5.7.1")
    api("org.junit.jupiter:junit-jupiter-params:5.7.1")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}
