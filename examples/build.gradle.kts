plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
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

    testImplementation("edu.cornell.cs.apl:test-utilities")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
