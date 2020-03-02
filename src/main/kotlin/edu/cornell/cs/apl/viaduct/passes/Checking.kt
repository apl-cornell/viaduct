package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errorskotlin.CompilationError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.Tree

/**
 * Performs all static checks on this program.
 *
 * @throws CompilationError if there are errors in the program.
 */
fun ProgramNode.check() {
    val nameAnalysis = NameAnalysis(Tree(this))
    nameAnalysis.check()
    TypeAnalysis(nameAnalysis).check()
    // TODO: information flow checking
}
