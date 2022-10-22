package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.OutParameterInitializationAnalysis
import io.github.apl_cornell.viaduct.analysis.TypeAnalysis
import io.github.apl_cornell.viaduct.errors.CompilationError
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.util.duration
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Check")

/**
 * Performs all static checks on this program.
 *
 * @throws CompilationError if there are errors in the program.
 */
fun ProgramNode.check() {
    logger.duration("name analysis") { NameAnalysis.get(this).check() }

    logger.duration("type analysis") { TypeAnalysis.get(this).check() }

    logger.duration("out parameter initialization analysis") { OutParameterInitializationAnalysis.get(this).check() }

    logger.duration("information flow analysis") { InformationFlowAnalysis.get(this).check() }
}
