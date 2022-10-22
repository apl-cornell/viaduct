package io.github.aplcornell.viaduct.syntax.surface

import io.github.aplcornell.viaduct.errors.InvalidConstructorCallError
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.concatenated
import io.github.aplcornell.viaduct.prettyprinting.joined
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.FunctionNameNode
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.JumpLabelNode
import io.github.aplcornell.viaduct.syntax.ObjectVariableNode
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.TemporaryNode
import io.github.aplcornell.viaduct.syntax.UpdateNameNode
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Modify
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Set
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

/**
 * A statement that is _not_ a combination of other statements, and that does not affect
 * control flow.
 *
 * Simple statements can show up in for loop headers.
 */
sealed class SimpleStatementNode : StatementNode()

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    val temporary: TemporaryNode,
    val value: ExpressionNode,
    val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document {
        val protocolDoc = protocol?.let {
            Document("@") + it.value.toDocument()
        } ?: Document("")

        return keyword("let") * temporary + protocolDoc * "=" * value
    }
}

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    val variable: ObjectVariableNode,
    val initializer: ExpressionNode,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document {
        val constructor: ConstructorCallNode =
            when (initializer) {
                is ConstructorCallNode -> initializer

                else -> throw InvalidConstructorCallError(initializer, constructorNeeded = true)
            }

        val typeAndProtocol = constructor.objectType.toDocument(constructor.protocol)

        return when (constructor.objectType.className.value) {
            ImmutableCell -> {
                keyword("val") * variable + Document(":") * typeAndProtocol * "=" * constructor.arguments[0]
            }

            MutableCell -> {
                keyword("var") * variable + Document(":") * typeAndProtocol * "=" * constructor.arguments[0]
            }

            else -> {
                val arguments = constructor.arguments.tupled().nested()
                keyword("val") * variable * "=" * typeAndProtocol + arguments
            }
        }
    }
}

/** An update method applied to an object. */
class UpdateNode(
    val variable: ObjectVariableNode,
    val update: UpdateNameNode,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document {
        val indexing = IndexingNode.from(this)
        return if (indexing != null) {
            val assignOp =
                if (update.value is Modify)
                    Document("${update.value.operator}=")
                else {
                    assert(update.value is Set)
                    Document("=")
                }
            indexing * assignOp * arguments.last()
        } else {
            variable + "." + update + arguments.tupled().nested()
        }
    }
}

/** Initialization for an out parameter. */
class OutParameterInitializationNode(
    val name: ObjectVariableNode,
    val rhs: ExpressionNode,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document = keyword("out") * name * Document("=") * rhs
}

/** Arguments to functions. */
sealed class FunctionArgumentNode : Node() {
    final override val comment: String?
        get() = null
}

/** Out arguments to functions. */
sealed class FunctionReturnArgumentNode : FunctionArgumentNode()

/** Function argument that is an expression. */
class ExpressionArgumentNode(
    val expression: ExpressionNode,
    override val sourceLocation: SourceLocation
) : FunctionArgumentNode() {
    override fun toDocumentWithoutComment(): Document = expression.toDocument()
}

/** Function argument that is an object reference (e.g. &a in the surface syntax). */
class ObjectReferenceArgumentNode(
    val variable: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionArgumentNode() {
    override fun toDocumentWithoutComment(): Document = Document("&${variable.value.name}")
}

/** Declaration of a new object as a return argument of a function. */
class ObjectDeclarationArgumentNode(
    val variable: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionReturnArgumentNode() {
    override fun toDocumentWithoutComment(): Document = keyword("val") * Document(variable.value.name)
}

/** Out parameter initialized as an out parameter to a function call. */
class OutParameterArgumentNode(
    val parameter: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionReturnArgumentNode() {
    override fun toDocumentWithoutComment(): Document = keyword("out") * Document(parameter.value.name)
}

/** Function call. */
class FunctionCallNode(
    val name: FunctionNameNode,
    val arguments: Arguments<FunctionArgumentNode>,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document = name + arguments.tupled()
}

/** A statement that does nothing. */
class SkipNode(
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document = keyword("skip")
}

// Communication Statements

/** An external output. */
class OutputNode(
    val message: ExpressionNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : SimpleStatementNode() {
    override fun toDocumentWithoutComment(): Document = keyword("output") * message * keyword("to") * host
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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : StatementNode() {
    override fun toDocumentWithoutComment(): Document =
        (keyword("if") * "(" + guard + ")") * thenBranch * keyword("else") * elseBranch
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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : LoopNode() {
    override fun toDocumentWithoutComment(): Document = keyword("loop") * body
}

/** Executing a statement repeatedly as long as a condition is true. */
class WhileLoopNode(
    val guard: ExpressionNode,
    override val body: BlockNode,
    override val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : LoopNode() {
    override fun toDocumentWithoutComment(): Document = (keyword("while") * "(" + guard + ")") * body
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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : LoopNode() {
    override fun toDocumentWithoutComment(): Document {
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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : StatementNode() {
    override fun toDocumentWithoutComment(): Document = keyword("break")
}

/** Asserting that a condition is true, and failing otherwise. */
class AssertionNode(
    val condition: ExpressionNode,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : StatementNode() {
    override fun toDocumentWithoutComment(): Document = keyword("assert") * condition
}

/** A sequence of statements. */
class BlockNode
private constructor(
    val statements: PersistentList<StatementNode>,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : StatementNode(), List<StatementNode> by statements {
    constructor(statements: List<StatementNode>, sourceLocation: SourceLocation, comment: String? = null) :
        this(statements.toPersistentList(), sourceLocation, comment)

    constructor(vararg statements: StatementNode, sourceLocation: SourceLocation, comment: String? = null) :
        this(persistentListOf(*statements), sourceLocation, comment)

    override fun toDocumentWithoutComment(): Document {
        val statements: List<Document> = statements.map {
            if (it is SimpleStatementNode || it is BreakNode || it is AssertionNode)
                it.toDocument() + ";"
            else
                it.toDocument()
        }
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return Document("{") +
            (Document.forcedLineBreak + body).nested() + Document.forcedLineBreak + "}"
    }
}
