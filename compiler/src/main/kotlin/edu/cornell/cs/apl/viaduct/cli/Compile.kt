package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backend.aby.ABYMuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessorRegistry
import edu.cornell.cs.apl.viaduct.passes.Splitter
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.validateProtocolAssignment
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import java.io.File
import java.io.StringWriter
import java.io.Writer
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

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

    val protocolSelectionOutput: File? by option(
        "-s",
        "--selection",
        metavar = "FILE.via",
        help = "Write program decorated with protocol selection to FILE.via"
    ).file(canBeDir = false)

    override fun run() {
        val unspecializedProgram = input.parse().elaborated()

        val program = unspecializedProgram.specialize()

        // Perform static checks.
        program.check()

        // Dump label constraint graph to a file if requested.
        dumpGraph(InformationFlowAnalysis.get(program)::exportConstraintGraph, constraintGraphOutput)

        val protocolFactory = SimpleProtocolFactory(program)

        // Select protocols.
        val protocolAssignment: (FunctionName, Variable) -> Protocol =
            selectProtocolsWithZ3(
                program,
                program.main,
                protocolFactory,
                SimpleCostEstimator
            ) { metadata -> dumpProgramMetadata(program, metadata, protocolSelectionOutput) }

        // Perform a sanity check to ensure the protocolAssignment is valid.
        // TODO: either remove this entirely or make it opt-in by the command line.
        validateProtocolAssignment(program, program.main, protocolFactory, protocolAssignment)

        val protocolAnalysis = ProtocolAnalysis(program, protocolAssignment, SimpleProtocolComposer)

        // Split the program.
        val splitProgram: ProgramNode = Splitter(protocolAnalysis).splitMain()

        // Post-process split program
        val postprocessor = ProgramPostprocessorRegistry(ABYMuxPostprocessor)
        val postprocessedProgram = postprocessor.postprocess(splitProgram)

        output.println(postprocessedProgram)
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
    when (file.extension.toLowerCase()) {
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
