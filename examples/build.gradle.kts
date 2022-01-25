plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
    application
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
    // Viaduct
    implementation("edu.cornell.cs.apl:runtime2")

    // Reflection
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")

    // Command-line-argument parsing
    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
}

application {
    mainClass.set("edu.cornell.cs.apl.viaduct.codegeneration.CodegenRunnerKt")
}
