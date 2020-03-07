package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.MethodNameNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/**
 * Thrown when an object is called with a method it does not recognize.
 *
 * @param objectName Receiver of the method.
 * @param methodName The unrecognized method.
 * @param objectType Inferred type of the object.
 * @param argumentTypes Types of the arguments the method was called with.
 * */
class UnknownMethodError(
    private val objectName: ObjectVariableNode,
    private val methodName: MethodNameNode,
    private val objectType: ObjectType,
    private val argumentTypes: List<ValueType>
) : CompilationError() {
    override val category: String
        get() = "Unknown Method"

    override val source: String
        get() = objectName.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This object does not have a method named") * methodName + Document(":")
                .withSource(objectName.sourceLocation) +
                Document("The object's type is:").withData(objectType) +
                Document("And the method's signature is:").withData(methodName + argumentTypes.tupled())
}
