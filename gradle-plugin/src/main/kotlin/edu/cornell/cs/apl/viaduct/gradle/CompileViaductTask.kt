package edu.cornell.cs.apl.viaduct.gradle

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class CompileViaductTask : DefaultTask() {
    @get:Incremental
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @Internal
    override fun getGroup(): String =
        LifecycleBasePlugin.BUILD_TASK_NAME

    @Internal
    override fun getDescription(): String =
        "Compiles Viaduct sources to Kotlin."

    @TaskAction
    @Suppress("UnstableApiUsage")
    fun compileAll(sourceChanges: InputChanges) {
        sourceChanges.getFileChanges(sourceDirectory).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val packagePath = change.file.parentFile.relativeTo(sourceDirectory.get().asFile)
            val kotlinPackage: String = packagePath.path.replace(File.separator, ".")
            val className: String = change.file.nameWithoutExtension
            val outputFile =
                outputDirectory.get().let { if (packagePath.path.isEmpty()) it else it.dir(packagePath.path) }
                    .file("$className.via").asFile

            when (change.changeType) {
                ChangeType.REMOVED -> {
                    logger.info("Deleting $outputFile.")
                    outputFile.delete()
                }
                else ->
                    compileFile(change.file, kotlinPackage, outputFile)
            }
        }
    }

    private fun compileFile(sourceFile: File, kotlinPackage: String, outputFile: File) {
        logger.info("Compiling from $sourceFile to $outputFile in package $kotlinPackage.")

        project.mkdir(outputFile.parentFile)
        edu.cornell.cs.apl.viaduct.main(
            listOf("compile", "-o", outputFile.path, sourceFile.path).toTypedArray()
        )
    }
}
