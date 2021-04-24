rootProject.name = "viaduct"
include("compiler")
include("cli")
include("runtime")
include("shared")
include("test-utilities")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // TODO: remove once kotlinx-html is on Maven Central
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
        maven { url = uri("https://jitpack.io") }
    }
}
