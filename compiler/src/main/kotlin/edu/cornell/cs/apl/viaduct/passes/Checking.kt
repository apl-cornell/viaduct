package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.OutParameterInitializationAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger("Check")

/**
 * Performs all static checks on this program.
 *
 * @throws CompilationError if there are errors in the program.
 */
fun ProgramNode.check() {
    logger.info { "name analysis..." }
    NameAnalysis.get(this).check()

    logger.info { "type checking..." }
    TypeAnalysis.get(this).check()

    logger.info { "out parameter initialization analysis..." }
    OutParameterInitializationAnalysis.get(this).check()

    val duration = measureTimeMillis {
        logger.info { "information flow analysis..." }
        InformationFlowAnalysis.get(this).check()
    }
    logger.info { "finished information flow analysis, ran for ${duration}ms" }
}
