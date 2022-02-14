package edu.cornell.cs.apl.viaduct.gradle

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.passes.compileToKotlin
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

abstract class CompileViaductTask : DefaultTask() {
    @get:Incremental
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val backend: Property<Backend>

    @Internal
    override fun getGroup(): String =
        LifecycleBasePlugin.BUILD_TASK_NAME

    @Internal
    override fun getDescription(): String =
        "Compiles Viaduct sources to Kotlin."

    @TaskAction
    fun compileAll(sourceChanges: InputChanges) {
        val errors: MutableList<CompilationError> = mutableListOf()
        sourceChanges.getFileChanges(sourceDirectory).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val packagePath = change.file.parentFile.toRelativeString(sourceDirectory.get().asFile).ifEmpty { "." }
            val packageName: String = packagePath.replace(File.separator, ".")
            val className: String = change.file.nameWithoutExtension
            val outputFile = outputDirectory.get().dir(packagePath).file("$className.kt").asFile

            when (change.changeType) {
                ChangeType.REMOVED -> {
                    logger.debug("Deleting $outputFile.")
                    outputFile.delete()
                }
                else ->
                    try {
                        compileFile(change.file, packageName, outputFile)
                    } catch (e: CompilationError) {
                        errors.add(e)
                    }
            }
        }
        errors.forEach {
            logger.error("e: $it")
        }
        if (errors.isNotEmpty()) {
            throw GradleException("Compilation error. See log for more details.")
        }
    }

    private fun compileFile(sourceFile: File, packageName: String, outputFile: File) {
        logger.debug("Compiling from $sourceFile to $outputFile in package $packageName.")

        val compiledProgram =
            SourceFile.from(sourceFile).compileToKotlin(
                fileName = outputFile.nameWithoutExtension,
                packageName = packageName,
                backend = backend.get()
            )

        // Write the output
        project.mkdir(outputFile.parentFile)
        outputFile.writer().use { compiledProgram.writeTo(it) }
    }
}
