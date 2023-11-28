package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.bracketed
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.ValueTypeNode

class ArrayTypeNode(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation,
) : Node() {
    override val children: Iterable<Node>
        get() = shape

    override fun toDocument(): Document = elementType + shape.bracketed()
}
