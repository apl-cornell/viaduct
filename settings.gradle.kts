rootProject.name = "viaduct"
include("cli")
include("compiler")
include("gradle-plugin")
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
