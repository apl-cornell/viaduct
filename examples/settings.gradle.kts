includeBuild("..")

pluginManagement {
    fun RepositoryHandler.githubPackage(packageName: String) {
        maven {
            name = packageName
            url = uri("https://maven.pkg.github.com/$packageName")
            credentials {
                username = "token"
                password =
                    "\u0037\u0066\u0066\u0036\u0030\u0039\u0033\u0066\u0032\u0037\u0033\u0036\u0033\u0037\u0064\u0036\u0037\u0066\u0038\u0030\u0034\u0039\u0062\u0030\u0039\u0038\u0039\u0038\u0066\u0034\u0066\u0034\u0031\u0064\u0062\u0033\u0064\u0033\u0038\u0065"
            }
        }
    }

    repositories {
        gradlePluginPortal()
        githubPackage("apl-cornell/aby-java")
    }
}

dependencyResolutionManagement {
    fun RepositoryHandler.githubPackage(packageName: String) {
        maven {
            name = packageName
            url = uri("https://maven.pkg.github.com/$packageName")
            credentials {
                username = "token"
                password =
                    "\u0037\u0066\u0066\u0036\u0030\u0039\u0033\u0066\u0032\u0037\u0033\u0036\u0033\u0037\u0064\u0036\u0037\u0066\u0038\u0030\u0034\u0039\u0062\u0030\u0039\u0038\u0039\u0038\u0066\u0034\u0066\u0034\u0031\u0064\u0062\u0033\u0064\u0033\u0038\u0065"
            }
        }
    }

    repositories {
        mavenCentral()
        githubPackage("apl-cornell/aby-java")
    }
}
