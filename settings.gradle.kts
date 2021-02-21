rootProject.name = "viaduct"
include("compiler")
include("cli")
include("runtime")
include("shared")
include("test-utilities")

dependencyResolutionManagement {
    repositories {
        jcenter()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
