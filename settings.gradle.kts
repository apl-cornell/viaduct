rootProject.name = "viaduct"
include("cli")
include("compiler")
include("docs")
include("gradle-plugin")
include("interpreter")
include("runtime")
include("shared")
include("test-utilities")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
