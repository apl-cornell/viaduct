package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.backend.aby.ABYMuxPostprocessor
import edu.cornell.cs.apl.viaduct.backend.zkp.ZKPMuxPostprocessor
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.codegeneration.BackendCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.PlainTextCodeGenerator
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessorRegistry
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.validateProtocolAssignment
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import mu.KotlinLogging
import java.io.File
import java.io.StringWriter
import java.io.Writer
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger("Compile")

class Compile : CliktCommand(help = "Compile ideal protocol to secure distributed program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    // TODO: option to print inferred label for each variable.
    // TODO: option to print selected protocol for each variable.

    val constraintGraphOutput: File? by option(
        "-c",
        "--constraint-graph",
        metavar = "FILE.EXT",
        help = """
            Write the generated label constraint graph to FILE.EXT

            File extension (EXT) determines the output format.
            Supported formats are the same as the ones in Graphviz.
            Most common ones are svg, png, dot, and json.
        """
    ).file(canBeDir = false)

    val labelOutput: File? by option(
        "-l",
        "--label",
        metavar = "FILE.via",
        help = "Write program decorated with minimal authority labels to FILE.via"
    ).file(canBeDir = false)

    val protocolSelectionOutput: File? by option(
        "-s",
        "--selection",
        metavar = "FILE.via",
        help = "Write program decorated with protocol selection to FILE.via"
    ).file(canBeDir = false)

    val maximizeCost: Boolean by option(
        "--maxcost",
        help = "Maximize cost during protocol selection instead of minimize"
    ).flag(default = false)

    val wanCost: Boolean by option(
        "--wancost",
        help = "Use WAN cost model instead of LAN cost model"
    ).flag(default = false)

    val compileKotlin: Boolean by option(
        "-k",
        "--compile-kotlin",
        help = "Translate .via source file to a .kt file"
    ).flag(default = false)

    override fun run() {
        logger.info { "elaborating source program..." }
        val unspecializedProgram = input.parse().elaborated()

        logger.info { "specializing functions..." }
        val program = unspecializedProgram.specialize()

        // Perform static checks.
        program.check()

        // Dump label constraint graph to a file if requested.
        val ifcAnalysis = InformationFlowAnalysis.get(program)
        dumpGraph(ifcAnalysis::exportConstraintGraph, constraintGraphOutput)

        if (labelOutput != null) {
            val labelMetadata: Map<Node, PrettyPrintable> =
                program.declarationNodes().map {
                    it to ifcAnalysis.label(it)
                }.plus(
                    program.letNodes().map {
                        it to ifcAnalysis.label(it)
                    }
                ).toMap()
            dumpProgramMetadata(program, labelMetadata, labelOutput)
        }

        val protocolFactory = DefaultCombinedBackend.protocolFactory(program)

        // Select protocols.
        logger.info { "selecting protocols..." }

        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val costRegime = if (wanCost) SimpleCostRegime.WAN else SimpleCostRegime.LAN
        val costEstimator = SimpleCostEstimator(protocolComposer, costRegime)

        val protocolAssignment: (FunctionName, Variable) -> Protocol
        val protocolSelectionDuration = measureTimeMillis {
            protocolAssignment = selectProtocolsWithZ3(
                program,
                protocolFactory,
                protocolComposer,
                costEstimator,
                if (maximizeCost) CostMode.MAXIMIZE else CostMode.MINIMIZE
            ) { metadata -> dumpProgramMetadata(program, metadata, protocolSelectionOutput) }
        }
        logger.info { "finished protocol selection, ran for ${protocolSelectionDuration}ms" }

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

        logger.info { "annotating program with protocols..." }
        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        // Post-process program
        val postprocessor = ProgramPostprocessorRegistry(
            ABYMuxPostprocessor(protocolAssignment),
            ZKPMuxPostprocessor(protocolAssignment)
        )
        val postprocessedProgram = postprocessor.postprocess(annotatedProgram)

        if (compileKotlin) {

            // TODO - figure out best way to let code generators know which protocols it is responsible for
            val backendCodeGenerator = BackendCodeGenerator(
                postprocessedProgram,
                listOf<(context: CodeGeneratorContext) -> CodeGenerator>(::PlainTextCodeGenerator),
                input!!.name.substringBefore('.'),
                "src"
            )

            val kotlin = backendCodeGenerator.generate()
            output.println(Document(kotlin))
        } else {
            output.println(postprocessedProgram)
        }
    }
}

/**
 * Outputs the graph generated by [graphWriter] to [file] if [file] is not `null`. Does nothing
 * otherwise. The output format is determined automatically from [file]'s extension.
 */
private fun dumpGraph(graphWriter: (Writer) -> Unit, file: File?) {
    if (file == null) {
        return
    }

    logger.info { "Writing graph to $file" }

    when (val format = formatFromFileExtension(file)) {
        Format.DOT ->
            file.bufferedWriter().use(graphWriter)
        else -> {
            val writer = StringWriter()
            graphWriter(writer)
            Graphviz.fromString(writer.toString()).render(format).toFile(file)
        }
    }
}

private fun dumpProgramMetadata(
    program: ProgramNode,
    metadata: Map<Node, PrettyPrintable>,
    file: File?
) {
    if (file == null) {
        return
    }

    logger.info { "Writing program metadata to $file" }
    file.println(program.printMetadata(metadata))
}

/** Infers Graphviz output format from [file]'s extension. */
private fun formatFromFileExtension(file: File): Format =
    when (file.extension.lowercase()) {
        "png" ->
            Format.PNG
        "svg" ->
            Format.SVG
        "dot" ->
            Format.DOT
        "xdot" ->
            Format.XDOT
        "txt" ->
            Format.PLAIN
        "ps" ->
            Format.PS2
        "json" ->
            Format.JSON0
        else ->
            throw UnknownGraphvizExtension(file)
    }

private class UnknownGraphvizExtension(file: File) :
    Error("Unknown Graphviz extension '${file.extension}' in $file.")
