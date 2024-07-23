package io.github.aplcornell.viaduct.gradle

import io.github.aplcornell.viaduct.backends.CircuitCodeGenerationBackend
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

        project.extensions.getByType<KotlinProjectExtension>().sourceSets.configureEach {
            val sourceSet = this.name
            val sourceDir = project.layout.projectDirectory.dir("src/$sourceSet/$PLUGIN_NAME")
            if (sourceDir.asFile.exists()) {
                val taskName = "compile${PLUGIN_NAME.capitalized()}${sourceSet.capitalized()}"
                val compileTask =
                    project.tasks.register<CompileViaductTask>(taskName) {
                        sourceDirectory.set(sourceDir)
                        outputDirectory.set(project.layout.buildDirectory.dir("generated/sources/$PLUGIN_NAME/$sourceSet"))
                        debugOutputDirectory.set(project.layout.buildDirectory.dir("$PLUGIN_NAME/$sourceSet"))
                        backend.set(CodeGenerationBackend)
                        circuitBackend.set(CircuitCodeGenerationBackend)
                    }

                kotlin.srcDir(compileTask.map { it.outputDirectory })
            }
        }
    }

    private companion object {
        const val PLUGIN_NAME = "viaduct"

        fun String.capitalized() = this.replaceFirstChar { it.uppercase() }
    }
}
