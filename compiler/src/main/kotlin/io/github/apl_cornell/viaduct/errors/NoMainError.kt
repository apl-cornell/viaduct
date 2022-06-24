package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.apl.prettyprinting.times
import io.github.apl_cornell.viaduct.analysis.mainFunction
import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode

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
