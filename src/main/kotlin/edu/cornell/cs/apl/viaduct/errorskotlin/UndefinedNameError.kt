package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import java.io.PrintStream

/** Thrown when a [Name] is referenced before it is defined. */
class UndefinedNameError(name: Located<Name>) : CompilationError() {
    private val name: Name = name.value
    private val location: SourceLocation = name.sourceLocation

    override val category: String
        get() = "Naming Error"

    override val source: String
        get() = location.sourcePath

    override fun print(output: PrintStream) {
        super.print(output)

        output.print("I cannot find a " + name.nameCategory + " named ")
        output.print(name.name) // TODO: Printer.run(name, output)
        output.println(':')

        output.println()
        location.showInSource(output)

        // TODO: show similar names in context
    }
}
