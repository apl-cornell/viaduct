package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.bracketed
import io.github.aplcornell.viaduct.prettyprinting.concatenated
import io.github.aplcornell.viaduct.prettyprinting.joined
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

sealed class CircuitStatementNode : StatementNode()

/** A sequence of circuit statements. */
class CircuitBlockNode
private constructor(
    val statements: PersistentList<CircuitStatementNode>,
    val returnStatement: ReturnNode,
    override val sourceLocation: SourceLocation,
) : Node(), List<CircuitStatementNode> by statements {
    constructor(statements: List<CircuitStatementNode>, returnStatement: ReturnNode, sourceLocation: SourceLocation) :
        this(statements.toPersistentList(), returnStatement, sourceLocation)

    override fun toDocument(): Document {
        val statements: MutableList<Document> = (statements.map { it.toDocument() } as MutableList<Document>)
        statements.add(returnStatement.toDocument())
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return Document("{") +
            (Document.forcedLineBreak + body).nested() + Document.forcedLineBreak + "}"
    }
}

/**
 * Binding the result of an expression to a variable.
 * Note that scalars are represented as arrays of dimension zero:
 *     val x = 5 ===> val x[] = 5
 */
class LetNode(
    override val name: VariableNode,
    val indices: Arguments<IndexParameterNode>,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : CircuitStatementNode(), VariableDeclarationNode {
    override fun toDocument(): Document =
        keyword("val") * name + indices.bracketed() * "=" * value
}

class ReturnNode(
    val values: Arguments<PureExpressionNode>,
    override val sourceLocation: SourceLocation,
) : StatementNode() {
    override fun toDocument(): Document = keyword("return") * values.joined()
}
