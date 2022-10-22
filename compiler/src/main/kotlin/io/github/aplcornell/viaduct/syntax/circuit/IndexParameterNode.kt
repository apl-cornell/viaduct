package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.SourceLocation

class IndexParameterNode(
    override val name: VariableNode,
    val bound: IndexExpressionNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override fun toDocument(): Document = name.toDocument() * "<" * bound
}
