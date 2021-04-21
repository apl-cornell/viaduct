rootProject.name = "viaduct"
include("compiler")
include("cli")
include("runtime")
include("shared")
include("test-utilities")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
