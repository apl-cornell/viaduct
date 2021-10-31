package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.analysis.mainFunction
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode

/** Thrown when trying to compile a program with no main. */
class NoMainError(override val source: String) : CompilationError() {
    override val category: String
        get() = "No Main"

    override val description: Document
        get() =
            Document("This program has no") * mainPhrase + "." + Document.lineBreak

    override val hint: Document
        get() = (Document("Add a") * mainPhrase * "like so:")
            .withData(exampleMain)

    private val exampleMain: ProgramNode
        get() = "fun ${mainFunction.name}() {}".parse()

    private val mainPhrase: Document
        get() =
            mainFunction * mainFunction.nameCategory
}
