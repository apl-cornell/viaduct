rootProject.name = "viaduct"
include("cli")
include("compiler")
include("docs")
include("gradle-plugin")
include("interpreter")
include("runtime")
include("shared")
include("test-utilities")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
