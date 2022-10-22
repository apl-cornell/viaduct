package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.analysis.InformationFlowAnalysis
import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.OutParameterInitializationAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.errors.CompilationError
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.util.duration
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
