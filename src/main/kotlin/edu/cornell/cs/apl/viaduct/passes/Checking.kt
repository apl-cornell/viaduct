package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errorskotlin.CompilationError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/**
 * Performs all static checks on this program.
 *
 * @throws CompilationError if there are errors in the program.
 */
fun ProgramNode.check() {
    val nameAnalysis = NameAnalysis(Tree(this))
    nameAnalysis.check()
    TypeAnalysis(nameAnalysis).check()
    InformationFlowAnalysis(nameAnalysis).check()
}
