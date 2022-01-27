plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")

    // Style checking
    id("com.diffplug.spotless") version "6.2.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("edu.cornell.cs.apl:runtime")
}

spotless {
    kotlinGradle {
        ktlint()
    }
    kotlin {
        val relativeBuildPath = project.buildDir.relativeTo(project.projectDir)
        targetExclude("$relativeBuildPath/**/*.kt")
        ktlint()
    }
}
