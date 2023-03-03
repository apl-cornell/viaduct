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
    implementation("com.github.ajalt.clikt:clikt:3.5.1")

    // Colored terminal output
    implementation("org.fusesource.jansi:jansi:2.4.0")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.18.1")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")

    // Logging
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.20.0"))
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")

    // Testing
    testImplementation(project(":test-utilities"))
}
