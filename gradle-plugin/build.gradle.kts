plugins {
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

dependencies {
    // TODO: use compiler directly instead of through the CLI.
    // implementation(project(":compiler"))
    implementation(project(":cli"))
}

gradlePlugin {
    plugins {
        register("viaduct-plugin") {
            id = "viaduct"
            implementationClass = "edu.cornell.cs.apl.viaduct.gradle.ViaductPlugin"
        }
    }
}
