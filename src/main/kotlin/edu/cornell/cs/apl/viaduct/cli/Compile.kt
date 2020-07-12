package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backend.ABYBackend
import edu.cornell.cs.apl.viaduct.backend.BackendCompiler
import edu.cornell.cs.apl.viaduct.backend.CommitmentBackend
import edu.cornell.cs.apl.viaduct.backend.PlaintextCppBackend
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.splitMain
import edu.cornell.cs.apl.viaduct.protocols.SimpleSelector
import edu.cornell.cs.apl.viaduct.protocols.simpleProtocolSort
import edu.cornell.cs.apl.viaduct.selection.GreedySelection
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
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

    val intermediate by option(
        "-i",
        "--intermediate",
        help = "Output intermediate representation instead of generating an executable"
    ).flag(default = false)

    override fun run() {
        val program = input.parse().elaborated()

        val nameAnalysis = NameAnalysis(Tree(program))
        val typeAnalysis = TypeAnalysis(nameAnalysis)
        val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

        // Dump label constraint graph to a file if requested.
        dumpGraph(informationFlowAnalysis::exportConstraintGraph, constraintGraphOutput)

        // Perform static checks.
        nameAnalysis.check()
        typeAnalysis.check()
        informationFlowAnalysis.check()

        // Select protocols.
        val protocolAssignment: (Variable) -> Protocol =
            GreedySelection(SimpleSelector(nameAnalysis, informationFlowAnalysis), ::simpleProtocolSort)
                .select(program.main, nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, protocolAssignment)

        // Split the program.
        val splitProgram: ProgramNode = program.splitMain(protocolAnalysis, typeAnalysis)

        if (!intermediate) {
            val backendCompiler = BackendCompiler(nameAnalysis, typeAnalysis)
            backendCompiler.registerBackend(PlaintextCppBackend(nameAnalysis, typeAnalysis))
            backendCompiler.registerBackend(ABYBackend(nameAnalysis, typeAnalysis))
            backendCompiler.registerBackend(CommitmentBackend(nameAnalysis, typeAnalysis))
            backendCompiler.compile(splitProgram, output)
        } else {
            output.println(splitProgram)
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
