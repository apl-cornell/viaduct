package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import java.io.PrintStream

/** Thrown when a [Name] has multiple declarations. */
class NameClashError(
    private val name: Name,
    private val firstDeclaration: SourceLocation,
    private val secondDeclaration: SourceLocation
) : CompilationError() {
    init {
        require(this.firstDeclaration.sourcePath == this.secondDeclaration.sourcePath)
    }

    override val category: String
        get() = "Name Clash"

    override val source: String
        get() = firstDeclaration.sourcePath

    override fun print(output: PrintStream) {
        super.print(output)

        output.print("This file has multiple declarations of ")
        output.print(name.name) // TODO: Printer.run(name, output)

        output.println(". One here:")

        output.println()
        firstDeclaration.showInSource(output)

        output.println("And another one here:")

        output.println()
        secondDeclaration.showInSource(output)
    }
}
