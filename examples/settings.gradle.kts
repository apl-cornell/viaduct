includeBuild("..")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        jcenter()
    }
}
