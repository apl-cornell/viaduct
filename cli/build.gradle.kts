plugins {
    application
    kotlin("jvm")
}

/** Application */

val mainPackage = "${project.group}.${rootProject.name}"

application {
    mainClass.set("$mainPackage.MainKt")
}

/** Dependencies */

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    // Command-line-argument parsing
    implementation("com.github.ajalt:clikt:2.8.0")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.1.1")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.18.0")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.0")

    // Logging
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.0")
}
