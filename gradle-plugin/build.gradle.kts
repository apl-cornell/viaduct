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
            implementationClass = "${(project.group as String).replace('-', '_')}.gradle.ViaductPlugin"
        }
    }
}

// TODO: remove this after Gradle 7.5 comes out.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = false
}

// TODO: remove this after Gradle 7.5 comes out.
afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.5"
            languageVersion = "1.5"
        }
    }
}
