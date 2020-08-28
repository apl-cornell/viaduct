import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")

    // Bug finding
    jacoco
}

dependencies {
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0-M1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0-M1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0-M1")
    implementation(kotlin("stdlib-jdk8"))
}

/** Testing */

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
    dependsOn(tasks.test)
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
