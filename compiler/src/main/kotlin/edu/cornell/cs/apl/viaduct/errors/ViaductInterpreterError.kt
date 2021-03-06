package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

class ViaductInterpreterError(
    val information: String,
    val node: HasSourceLocation? = null
) : CompilationError() {
    override val category = "Interpreter Error"

    override val source: String = node?.sourceLocation?.sourcePath ?: ""

    override val description: Document
        get() = if (node == null) (Document(information)) else Document(information).withSource(node.sourceLocation)
}
