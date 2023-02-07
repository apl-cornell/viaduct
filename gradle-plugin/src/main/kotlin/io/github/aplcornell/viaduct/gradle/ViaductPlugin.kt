package io.github.aplcornell.viaduct.gradle

import io.github.aplcornell.viaduct.backends.CodeGenerationBackend
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class ViaductPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<ViaductPluginExtension>("viaduct")

        val backends = CodeGenerationBackend

        project.extensions.getByType<KotlinProjectExtension>().sourceSets.configureEach {
            val sourceSet = this.name
            val sourceDir = project.layout.projectDirectory.dir("src/$sourceSet/$pluginName")
            if (sourceDir.asFile.exists()) {
                val taskName = "compile${pluginName.capitalized()}${sourceSet.capitalized()}"
                val compileTask = project.tasks.register<CompileViaductTask>(taskName) {
                    sourceDirectory.set(sourceDir)
                    outputDirectory.set(project.layout.buildDirectory.dir("generated/sources/$pluginName/$sourceSet"))
                    debugOutputDirectory.set(project.layout.buildDirectory.dir("$pluginName/$sourceSet"))
                    backend.set(backends)
                }

                kotlin.srcDir(compileTask.map { it.outputDirectory })
            }
        }
    }

    private companion object {
        const val pluginName = "viaduct"

        fun String.capitalized() =
            this.replaceFirstChar { it.uppercase() }
    }
}
