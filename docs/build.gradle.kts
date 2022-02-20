val buildApiDocs: TaskProvider<Task> = rootProject.tasks.named("dokkaHtmlMultiModule")

val mkdocsBuild by tasks.registering {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Build documentation."

    dependsOn(buildApiDocs)

    doLast {
        pipenvRun("mkdocs", "build")
    }
}

val mkdocsServe by tasks.registering {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Serves documentation locally."

    dependsOn(buildApiDocs)

    doLast {
        pipenvRun("mkdocs", "serve")
    }
}

val mkdocsPublish by tasks.registering {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Publishes documentation to GitHub pages."

    dependsOn(buildApiDocs)

    doLast {
        pipenvRun(
            "mike",
            "deploy",
            "--rebase",
            "--push",
            "--update-aliases",
            "$version",
            "latest"
        )
    }
}

/** Executes `pipenv` with [arguments]. */
fun pipenv(vararg arguments: String) {
    exec {
        executable("pipenv")
        args(*arguments)
    }
}

/** Runs [command] using `pipenv`. */
fun pipenvRun(vararg command: String) {
    pipenv("sync")
    pipenv("run", *command)
}
