package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.bracketed
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.ValueTypeNode
import io.github.aplcornell.viaduct.syntax.types.Type

class ArrayType(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
) : Type {
    override fun toDocument(): Document = elementType + shape.bracketed()
}
