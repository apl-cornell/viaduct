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
        register("${rootProject.name}-plugin") {
            id = project.group as String
            implementationClass = "${project.group}.gradle.ViaductPlugin"
        }
    }
}
