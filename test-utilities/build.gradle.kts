plugins {
    kotlin("jvm")
}

/** Dependencies */

dependencies {
    implementation(project(":compiler"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    implementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
}
