package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName

/**
 * Thrown when an object is declared with an unknown datatype (class name).
 *
 * @param objectName Receiver of the method.
 * @param className The unrecognized method.
 */
class UnknownDatatypeError(
    private val objectName: ObjectVariableNode,
    private val className: ClassName
) : CompilationError() {
    override val category: String
        get() = "Unknown Datatype"

    override val source: String
        get() = objectName.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This object has an unknown datatype named") * className + Document(":")
                .withSource(objectName.sourceLocation)
}
