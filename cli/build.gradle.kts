plugins {
    application
    kotlin("jvm")
}

/** Application */

val rootPackage: String by ext
val mainPackage = "$rootPackage.${project.name}"

application {
    mainClass.set("$mainPackage.MainKt")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))
    implementation(project(":interpreter"))

    // Command-line-argument parsing
    implementation(libs.clikt)

    // Colored terminal output
    implementation(libs.jansi)

    // DOT graph output
    implementation(libs.graphviz.java)
    implementation(libs.graphviz.java.all.j2v8)

    // Logging
    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j2.impl)

    // Testing
    testImplementation(project(":test-utilities"))
}
