plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.dokka") version "0.10.0"

    // Style checking
    checkstyle
    id("com.diffplug.gradle.spotless") version "3.18.0"
    id("org.ec4j.editorconfig") version "0.0.3"

    // Bug finding
    jacoco
    id("com.github.spotbugs") version "1.6.9"

    // Lexing & Parsing
    id("org.xbib.gradle.plugin.jflex") version "1.2.0"
    id("cup.gradle.cup-gradle-plugin") version "1.2"
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.diffplug.gradle.spotless")

    group = "edu.cornell.cs.apl"

    version = "0.1"

    repositories {
        jcenter()
    }

    /** Compilation */

    tasks.compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    /** Code Style */

    spotless {
        kotlin {
            ktlint()
        }
    }

    /** Testing */

    tasks.test {
        useJUnitPlatform()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    }
}


/** Application */

application {
    mainClassName = "${project.group}.${project.name}.Main"
}

tasks.jar {
    manifest {
        attributes(Pair("Main-Class", application.mainClassName))
    }
}


/** Dependencies */

dependencies {
    // Subprojects
    api(project(":prettyprinting"))

    // Standard libraries
    implementation(kotlin("stdlib-jdk8"))

    // Functional data structures
    implementation("io.vavr:vavr:0.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")

    // Google's Guava (core data structures)
    implementation("com.google.guava:guava:28.1-jre")

    // Google's AutoValue for creating immutable classes
    implementation("com.google.auto.value:auto-value-annotations:1.6.2")
    annotationProcessor("com.google.auto.value:auto-value:1.6.2")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.3.1")
    implementation("org.jgrapht:jgrapht-io:1.3.1")

    // Unicode support
    implementation("com.ibm.icu:icu4j:64.2")

    // Command-line-argument parsing
    implementation("com.github.rvesse:airline:2.7.0")

    // Colored terminal output
    // TODO: remove from here if you can or move to general
    implementation("org.fusesource.jansi:jansi:1.18")

    // DOT graph output
    implementation("guru.nidi:graphviz-java:0.11.0")

    // Logging (disabled for now using the NOP engine)
    implementation("org.slf4j:slf4j-nop:1.8.0-beta4")
}


/** Compilation */

tasks.compileKotlin {
    dependsOn(tasks.cupCompile)
    dependsOn(tasks.jflex)
}

cup {
    setArgs("-parser", "ImpParser", "-interface")
}

tasks.cupCompile {
    loadConfig()
    sourceSets.main {
        java.srcDir(generateDir)
    }
}


/** Checks */

// TODO: remove once we convert all Java code
checkstyle {
    maxWarnings = 0
    configDir = project.file("config/checkstyle")
}

spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
    }
}

// TODO: remove once we convert all Java code
tasks.withType<com.github.spotbugs.SpotBugsTask> {
    reports {
        xml.isEnabled = false
        html.isEnabled = true
    }
    excludeFilter = project.file("config/spotbugs/excludeFilter.xml")
}

editorconfig {
    excludes = listOf("$buildDir", "out", "**/*.hprof")
}

tasks.check {
    dependsOn(tasks.editorconfigCheck)
}


/** Testing */

tasks.test {
    useJUnitPlatform()

    // Rerun tests when code examples change.
    inputs.files(project.fileTree("examples"))
    inputs.files(project.fileTree("errors"))
}

// TODO: this does not cover subprojects for now.
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.named<JavaExec>("run") {
    enableAssertions = true
}


/** Documentation */

// Kotlin documentation
tasks.dokka {
    outputFormat = "gfm"
    outputDirectory = "$buildDir/docs"
    subProjects = subprojects.map { it.name }

    configuration {
        includes = listOf("packages.md")
    }
}
