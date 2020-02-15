package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.parsing.referenceFrom
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

/**
 * A statement that is _not_ a combination of other statements.
 *
 * Simple statements can show up in for loop headers.
 */
sealed class SimpleStatementNode : StatementNode()

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    val temporary: TemporaryNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val asDocument: Document
        get() = keyword("let") * temporary * "=" * value
}

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    val variable: ObjectVariableNode,
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    // TODO: allow leaving out some of the labels (right now it's all or nothing)
    val labelArguments: Arguments<Located<Label>>?,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val asDocument: Document
        get() =
            when (className.value) {
                MutableCell -> {
                    val label = labelArguments?.get(0) ?: Document()
                    typeArguments[0] + label * variable * "=" * arguments[0]
                }

                Vector -> {
                    // TODO: change array syntax to be the same as other objects
                    val label = labelArguments?.get(0) ?: Document()
                    typeArguments[0] + label * variable + "[" + arguments[0] + "]"
                }
                else -> {
                    val types = typeArguments.bracketed().nested()
                    val labels = labelArguments?.braced()?.nested() ?: Document()
                    val arguments = arguments.tupled().nested()
                    // TODO: This may not work due to restrictions in parsers
                    keyword("let") * variable * "=" * className + types + labels + arguments
                }
            }
}

/** An update method applied to an object. */
class UpdateNode(
    val variable: ObjectVariableNode,
    val update: UpdateName,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val asDocument: Document
        get() {
            val reference = referenceFrom(this)
            return if (reference != null) {
                val assignOp =
                    if (update is Modify)
                        Document(update.operator.toString()) + "="
                    else {
                        assert(update is Set)
                        Document("=")
                    }
                reference * assignOp * arguments.last()
            } else {
                variable + "." + update + arguments.tupled().nested()
            }
        }
}

/** A statement that does nothing. */
class SkipNode(override val sourceLocation: SourceLocation) : SimpleStatementNode() {
    override val asDocument: Document
        get() = keyword("skip")
}

// Compound Statements

/**
 * Executing statements conditionally.
 *
 * @param thenBranch Statement to execute if the guard is true.
 * @param elseBranch Statement to execute if the guard is false.
 */
class IfNode(
    val guard: ExpressionNode,
    val thenBranch: BlockNode,
    val elseBranch: BlockNode,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    override val asDocument: Document
        get() = (keyword("if") * "(" + guard + ")") * thenBranch * "else" * elseBranch
}

/** A loop statement. */
sealed class LoopNode : StatementNode() {
    /** A label for the loop that break nodes can refer to. */
    abstract val jumpLabel: JumpLabelNode?

    /** Statements to execute repeatedly. */
    abstract val body: BlockNode
}

/** Executing a statement until a break statement is encountered. */
class InfiniteLoopNode(
    override val body: BlockNode,
    override val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation
) : LoopNode() {
    override val asDocument: Document
        get() = keyword("loop") * body
}

/** Executing a statement repeatedly as long as a condition is true. */
class WhileLoopNode(
    val guard: ExpressionNode,
    override val body: BlockNode,
    override val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation
) : LoopNode() {
    override val asDocument: Document
        get() = (keyword("while") * "(" + guard + ")") * body
}

/**
 * A for loop.
 *
 * @param initialize Initializer for loop variables.
 * @param guard Loop until this becomes false.
 * @param update Update loop variables after each iteration.
 */
class ForLoopNode(
    val initialize: SimpleStatementNode,
    val guard: ExpressionNode,
    val update: SimpleStatementNode,
    override val body: BlockNode,
    override val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation
) : LoopNode() {
    override val asDocument: Document
        get() {
            val header: Document =
                listOf(initialize, guard, update).joined(
                    separator = Document(";"),
                    prefix = Document("("),
                    postfix = Document(")")
                )
            return keyword("for") * header * body
        }
}

/**
 * Breaking out of a loop.
 *
 * @param jumpLabel Label of the loop to break out of. A null value refers to the innermost loop.
 */
class BreakNode(
    val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    override val asDocument: Document
        get() = keyword("break")
}

/** A sequence of statements. */
class BlockNode(
    statements: List<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode(), List<StatementNode> by statements {
    // Make an immutable copy
    val statements: List<StatementNode> = statements.toPersistentList()

    constructor(vararg statements: StatementNode, sourceLocation: SourceLocation) :
        this(persistentListOf(*statements), sourceLocation)

    override val asDocument: Document
        get() {
            val statements: List<Document> = statements.map {
                if (it is SimpleStatementNode) it.asDocument + ";" else it.asDocument
            }
            val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
            return Document("{") +
                (Document.forcedLineBreak + body).nested() + Document.forcedLineBreak + "}"
        }
}

// Communication Statements

/** An external output. */
class OutputNode(
    val message: ExpressionNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val asDocument: Document
        get() = keyword("output") * message * keyword("to") * host
}

/** Sending a value to another protocol. */
class SendNode(
    val message: ExpressionNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val asDocument: Document
        get() = keyword("send") * message * keyword("to") * protocol
}
