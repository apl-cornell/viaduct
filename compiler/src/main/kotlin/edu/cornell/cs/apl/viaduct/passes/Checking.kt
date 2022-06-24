package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.OutParameterInitializationAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.duration
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
