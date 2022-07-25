package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.SourceLocation

class IndexParameterNode(
    override val name: VariableNode,
    val bound: IndexExpressionNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override fun toDocument(): Document = name.toDocument() * "<" * bound
}
