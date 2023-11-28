package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.SourceLocation

class IndexParameterNode(
    override val name: VariableNode,
    val bound: IndexExpressionNode,
    override val sourceLocation: SourceLocation,
) : Node(), VariableDeclarationNode {
    override val children: Iterable<Node>
        get() = listOf(bound)

    override fun toDocument(): Document = name.toDocument() * "<" * bound
}
