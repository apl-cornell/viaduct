package edu.cornell.cs.apl.viaduct.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class ViaductPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<ViaductPluginExtension>("viaduct")
        println("Backends: ${extension.backends}")
        val compileViaduct = project.tasks.register<CompileViaductTask>("compileViaduct") {
            sourceDirectory.set(project.layout.projectDirectory.dir("src/main/viaduct"))
            outputDirectory.set(project.layout.buildDirectory.dir("generated/sources/viaduct"))
        }

        project.tasks.getByName("build").dependsOn(compileViaduct)
    }
}
