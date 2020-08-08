package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/**
 * Performs all static checks on this program.
 *
 * @throws CompilationError if there are errors in the program.
 */
fun ProgramNode.check() {
    NameAnalysis.get(this).check()
    TypeAnalysis.get(this).check()
    InformationFlowAnalysis.get(this).check()
}
