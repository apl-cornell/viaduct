plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("viaduct")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("edu.cornell.cs.apl:runtime2")
}
