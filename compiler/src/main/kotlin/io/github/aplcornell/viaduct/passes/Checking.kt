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
    logger.duration("name analysis") { analyses.get<NameAnalysis>().check() }

    logger.duration("type analysis") { analyses.get<TypeAnalysis>().check() }

    logger.duration("out parameter initialization analysis") {
        analyses.get<OutParameterInitializationAnalysis>().check()
    }

    logger.duration("information flow analysis") { analyses.get<InformationFlowAnalysis>().check() }
}
