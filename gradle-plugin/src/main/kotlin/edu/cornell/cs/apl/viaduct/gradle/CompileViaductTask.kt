package edu.cornell.cs.apl.viaduct.gradle

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.CommitmentDispatchCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.PlainTextCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.compileKotlinFile
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelection
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.Z3Selection
import edu.cornell.cs.apl.viaduct.selection.validateProtocolAssignment
import org.gradle.api.DefaultTask
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
                    compileFile(change.file, packageName, outputFile)
            }
        }
    }

    private fun compileFile(sourceFile: File, packageName: String, outputFile: File) {
        logger.debug("Compiling from $sourceFile to $outputFile in package $packageName.")

        val compiledProgram = try {
            compile(sourceFile, packageName, outputFile.nameWithoutExtension)
        } catch (e: CompilationError) {
            throw Error(e.toString(), e)
        }

        // Write the output
        project.mkdir(outputFile.parentFile)
        outputFile.writeText(compiledProgram)
    }

    private fun compile(sourceFile: File, packageName: String, fileName: String): String {
        val program = SourceFile.from(sourceFile).parse().elaborated().specialize()

        // Perform static checks.
        program.check()

        // TODO: don't bake in cost regime
        val protocolFactory = backend.get().protocolFactory(program)
        val protocolComposer = backend.get().protocolComposer
        val costEstimator = SimpleCostEstimator(protocolComposer, SimpleCostRegime.WAN)
        val protocolAssignment =
            ProtocolSelection(
                Z3Selection(),
                protocolFactory,
                protocolComposer,
                costEstimator
            ).selectAssignment(program)

        // Perform a sanity check to ensure the protocolAssignment is valid.
        // TODO: either remove this entirely or make it opt-in by the command line.
        validateProtocolAssignment(
            program,
            protocolFactory,
            protocolComposer,
            costEstimator,
            protocolAssignment
        )

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        return compileKotlinFile(
            annotatedProgram,
            fileName,
            packageName,
            listOf<(context: CodeGeneratorContext) -> CodeGenerator>(
                ::PlainTextCodeGenerator,
                ::CommitmentDispatchCodeGenerator
            ),
            backend.get().protocolComposer
        )
    }
}
