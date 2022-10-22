package io.github.aplcornell.viaduct.gradle

import io.github.aplcornell.viaduct.backends.CodeGenerationBackend
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class ViaductPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<ViaductPluginExtension>("viaduct")

        val backends = CodeGenerationBackend

        // TODO: this should use source sets
        val compileViaduct = project.tasks.register<CompileViaductTask>("compileViaduct") {
            sourceDirectory.set(project.layout.projectDirectory.dir("src/main/viaduct"))
            outputDirectory.set(project.layout.buildDirectory.dir("generated/sources/viaduct"))
            debugOutputDirectory.set(project.layout.buildDirectory.dir("viaduct"))
            backend.set(backends)
        }

        // TODO: this should be done by source set
        val main = SourceSet.MAIN_SOURCE_SET_NAME
        project.extensions.getByType<KotlinProjectExtension>().sourceSets.named(main).configure {
            kotlin.srcDir(compileViaduct.map { it.outputDirectory })
        }
    }
}
