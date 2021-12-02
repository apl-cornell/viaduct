plugins {
    application
    kotlin("jvm")
}

/** Application */

val mainPackage = "${project.group}.${rootProject.name}.${project.name}"

application {
    mainClass.set("$mainPackage.MainKt")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))
    implementation(project(":runtime"))

    // Command-line-argument parsing
    implementation("com.github.ajalt.clikt:clikt:3.3.0")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.4.0")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.18.1")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")

    // Logging
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")

    // Testing
    testImplementation(project(":test-utilities"))
}
