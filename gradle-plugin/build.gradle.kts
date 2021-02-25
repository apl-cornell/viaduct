plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":compiler"))

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

gradlePlugin {
    plugins {
        register("viaduct-plugin") {
            id = "viaduct"
            implementationClass = "edu.cornell.cs.apl.viaduct.gradle.ViaductPlugin"
        }
    }
}
