package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Thrown when protocol selection cannot find a solution. */
class NoSelectionSolutionError(private val program: ProgramNode) : CompilationError() {
    override val category: String
        get() = "Protocol Selection"

    override val source: String
        get() = program.sourceLocation.sourcePath

    override val description: Document
        get() = Document("Cannot find a protocol assignment for this program.\n")
}
