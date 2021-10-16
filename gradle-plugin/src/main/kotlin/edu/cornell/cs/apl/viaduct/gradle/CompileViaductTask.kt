package edu.cornell.cs.apl.viaduct.gradle

import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.codegeneration.viaductProgramStringGenerator
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.validateProtocolAssignment
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
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
import java.io.File

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
        val protocolFactory = SimpleProtocolFactory(program)
        val protocolComposer = SimpleProtocolComposer
        val costEstimator = SimpleCostEstimator(protocolComposer, SimpleCostRegime.WAN)

        val protocolAssignment = selectProtocolsWithZ3(
            program,
            program.main,
            protocolFactory,
            protocolComposer,
            costEstimator,
            CostMode.MINIMIZE
        )

        // Perform a sanity check to ensure the protocolAssignment is valid.
        // TODO: either remove this entirely or make it opt-in by the command line.
        for (processDecl in program.declarations.filterIsInstance<ProcessDeclarationNode>()) {
            validateProtocolAssignment(
                program,
                processDecl,
                protocolFactory,
                protocolComposer,
                costEstimator,
                protocolAssignment
            )
        }

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        // TODO: code generator should return a KotlinPoet class
        return viaductProgramStringGenerator(annotatedProgram, fileName, packageName)
    }
}
