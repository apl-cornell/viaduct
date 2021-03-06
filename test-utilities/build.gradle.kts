plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    api("org.junit.jupiter:junit-jupiter-api:5.7.2")
    api("org.junit.jupiter:junit-jupiter-params:5.7.2")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}
