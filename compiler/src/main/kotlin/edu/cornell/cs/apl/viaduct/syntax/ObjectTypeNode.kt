package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell

/** A class name applied to type and label arguments. */
class ObjectTypeNode(
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    val labelArguments: Arguments<LabelNode>?
) : HasSourceLocation, PrettyPrintable {
    override val sourceLocation: SourceLocation
        get() = className.sourceLocation.merge((labelArguments ?: typeArguments).sourceLocation)

    override fun toDocument(): Document {
        val labels = labelArguments?.braced()?.nested() ?: Document()
        return if (className.value == ImmutableCell || className.value == MutableCell) {
            val types = typeArguments.joined().nested()
            types + labels
        } else {
            val types = typeArguments.bracketed().nested()
            className + types + labels
        }
    }

    /** Pretty prints this object type along with protocol annotation. */
    fun toDocument(protocol: ProtocolNode?): Document =
        toDocument() + (protocol?.let { Document("@") + it.value } ?: Document(""))
}
