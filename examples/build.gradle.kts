plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("edu.cornell.cs.apl:runtime2")
}
