package io.github.apl_cornell.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import io.github.apl_cornell.viaduct.backends.CodeGenerationBackend
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.passes.compile
import io.github.apl_cornell.viaduct.passes.compileToKotlin
import io.github.apl_cornell.viaduct.selection.SimpleCostRegime
import mu.KotlinLogging
import java.io.File
import java.io.StringWriter
import java.io.Writer

private val logger = KotlinLogging.logger("Compile")

class Compile : CliktCommand(help = "Compile ideal protocol to secure distributed program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    // TODO: option to print inferred label for each variable.
    // TODO: option to print selected protocol for each variable.

    val constraintGraphOutput: File? by option(
        "-cg",
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

    val costOutput: File? by option(
        "-c",
        "--cost",
        metavar = "FILE.via",
        help = "Write program decorated with cost to FILE.via"
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
        "--kotlin",
        help = "Translate .via source file to a .kt file"
    ).flag(default = false)

    override fun run() {
        val costRegime = if (wanCost) SimpleCostRegime.WAN else SimpleCostRegime.LAN

        if (compileKotlin) {
            val compiledProgram =
                input.sourceFile().compileToKotlin(
                    fileName = output?.nameWithoutExtension ?: "Source",
                    packageName = ".",
                    backend = CodeGenerationBackend,
                    costRegime = costRegime,
                    saveLabelConstraintGraph = constraintGraphOutput::dumpGraph,
                    saveInferredLabels = labelOutput,
                    saveEstimatedCost = costOutput,
                    saveProtocolAssignment = protocolSelectionOutput
                )
            output.write(compiledProgram)
        } else {
            val compiledProgram =
                input.sourceFile().compile(
                    backend = DefaultCombinedBackend,
                    costRegime = costRegime,
                    saveLabelConstraintGraph = constraintGraphOutput::dumpGraph,
                    saveInferredLabels = labelOutput,
                    saveEstimatedCost = costOutput,
                    saveProtocolAssignment = protocolSelectionOutput
                )
            output.println(compiledProgram)
        }
    }
}

/**
 * Outputs the graph generated by [graphWriter] to [file] if [file] is not `null`. Does nothing
 * otherwise. The output format is determined automatically from [file]'s extension.
 */
private fun File?.dumpGraph(graphWriter: (Writer) -> Unit) {
    if (this == null) return

    logger.info { "Writing label constraint graph to $this." }

    when (val format = formatFromFileExtension(this)) {
        Format.DOT ->
            this.bufferedWriter().use(graphWriter)
        else -> {
            val writer = StringWriter()
            graphWriter(writer)
            Graphviz.fromString(writer.toString()).render(format).toFile(this)
        }
    }
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