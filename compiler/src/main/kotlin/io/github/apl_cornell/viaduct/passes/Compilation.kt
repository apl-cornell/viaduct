package io.github.apl_cornell.viaduct.passes

import com.squareup.kotlinpoet.FileSpec
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis2
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.descendantsIsInstance
import io.github.apl_cornell.viaduct.backends.Backend
import io.github.apl_cornell.viaduct.backends.aby.abyMuxPostprocessor
import io.github.apl_cornell.viaduct.backends.zkp.zkpMuxPostprocessor
import io.github.apl_cornell.viaduct.codegeneration.compileToKotlin
import io.github.apl_cornell.viaduct.parsing.SourceFile
import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.selection.ProtocolSelection
import io.github.apl_cornell.viaduct.selection.SimpleCostEstimator
import io.github.apl_cornell.viaduct.selection.SimpleCostRegime
import io.github.apl_cornell.viaduct.selection.Z3Selection
import io.github.apl_cornell.viaduct.selection.validateProtocolAssignment
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.Metadata
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.util.duration
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.io.Writer

private val logger = KotlinLogging.logger("Compile")

/** Similar to [SourceFile.compileToKotlin], but returns a program for the interpreter. */
fun SourceFile.compile(
    backend: Backend,
    costRegime: SimpleCostRegime = SimpleCostRegime.WAN,
    saveLabelConstraintGraph: ((graphWriter: (Writer) -> Unit) -> Unit)? = null,
    saveInferredLabels: File? = null,
    saveEstimatedCost: File? = null,
    saveProtocolAssignment: File? = null
): ProgramNode {
    val program = run {
        val parsed = logger.duration("parsing") {
            this.parse(backend.protocolParsers)
        }
        val elaborated = logger.duration("elaboration") {
            parsed.elaborated()
        }
        // TODO: specialization fails with a null pointer error without this redundant check.
        NameAnalysis.get(elaborated).check()
        logger.duration("function specialization") {
            elaborated.specialize()
        }
    }

    // Perform static checks.
    program.check()

    // Dump label constraint graph.
    saveLabelConstraintGraph?.invoke(InformationFlowAnalysis2.get(program)::exportConstraintGraph)

    // Dump program annotated with inferred labels.
    if (saveInferredLabels != null) {
        val ifcAnalysis = InformationFlowAnalysis2.get(program)
        val labelMetadata: Metadata = sequence {
            yieldAll(program.descendantsIsInstance<LetNode>().map { it to ifcAnalysis.label(it) })
            yieldAll(program.descendantsIsInstance<DeclarationNode>().map { it to ifcAnalysis.label(it) })
        }.toMap()
        saveInferredLabels.dumpProgramMetadata(program, labelMetadata)
    }

    // Select protocols.
    val protocolFactory = backend.protocolFactory(program)
    val protocolComposer = backend.protocolComposer
    val costEstimator = SimpleCostEstimator(protocolComposer, costRegime)
    val protocolAssignment = logger.duration("protocol selection") {
        ProtocolSelection(
            Z3Selection(),
            protocolFactory,
            protocolComposer,
            costEstimator
        ).selectAssignment(program)
    }

    // Dump program annotated with cost information.
    if (saveEstimatedCost != null) {
        val costMetadata: Metadata =
            protocolAssignment.problem.costMap.mapValues { kv ->
                Document(protocolAssignment.evaluate(kv.value).toString())
            }
        saveEstimatedCost.dumpProgramMetadata(program, costMetadata)
    }

    // Perform a sanity check to ensure the protocol assignment is valid.
    // TODO: either remove this entirely or make it opt-in by the command line.
    validateProtocolAssignment(
        program,
        protocolFactory,
        protocolComposer,
        costEstimator,
        protocolAssignment
    )

    val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

    // Dump protocol assignment.
    saveProtocolAssignment?.println(annotatedProgram)

    // Post-process program
    val postProcessedProgram = logger.duration("post processing") {
        val postprocessor = ProgramPostprocessorRegistry(
            abyMuxPostprocessor(protocolAssignment),
            zkpMuxPostprocessor(protocolAssignment)
        )
        postprocessor.postprocess(annotatedProgram)
    }

    return postProcessedProgram
}

/**
 * Compile [this] source file to a Kotlin program.
 *
 * Supports writing intermediate results to files for debugging.
 * To do so, set `save` parameters to a file name.
 *
 * @param backend Cryptographic backends to use.
 * @param costRegime Cost model to use for protocol selection.
 * @param saveLabelConstraintGraph Output information flow constraint graph (in DOT format).
 * @param saveInferredLabels Output program decorated with inferred information flow labels.
 * @param saveEstimatedCost Output program decorated with estimated cost information.
 * @param saveProtocolAssignment Output program decorated with selected protocols.
 */
fun SourceFile.compileToKotlin(
    fileName: String,
    packageName: String,
    backend: Backend,
    costRegime: SimpleCostRegime = SimpleCostRegime.WAN,
    saveLabelConstraintGraph: ((graphWriter: (Writer) -> Unit) -> Unit)? = null,
    saveInferredLabels: File? = null,
    saveEstimatedCost: File? = null,
    saveProtocolAssignment: File? = null
): FileSpec {
    val postProcessedProgram =
        this.compile(
            backend,
            costRegime,
            saveLabelConstraintGraph,
            saveInferredLabels,
            saveEstimatedCost,
            saveProtocolAssignment
        )

    return postProcessedProgram.compileToKotlin(
        fileName,
        packageName,
        backend::codeGenerator,
        backend.protocolComposer
    )
}

private fun File.dumpProgramMetadata(program: ProgramNode, metadata: Metadata) {
    logger.info { "Writing program metadata to $this." }
    this.println(program.toDocumentWithMetadata(metadata))
}

/**
 * Pretty prints [document] (plus the line separator) to [this] file.
 *
 * @throws IOException
 */
private fun File.println(document: PrettyPrintable) {
    val doc = document.toDocument() + Document.lineBreak
    PrintStream(this, Charsets.UTF_8).use { doc.print(it, ansi = false) }
}
