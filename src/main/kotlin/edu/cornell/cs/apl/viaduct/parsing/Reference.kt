package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode

/**
 * A pointer to a location such as the contents of a mutable cell or a specific index in an array.
 *
 * Used to unify reading from and writing to locations in the parser.
 */
internal interface Reference : PrettyPrintable {
    /** Returns an expression that reads the value stored at the referenced location. */
    fun get(): QueryNode

    /**
     * Returns a statement that changes the value stored at the referenced location to the
     * result of [value].
     */
    fun set(value: ExpressionNode): UpdateNode

    /**
     * Returns a statement that changes the value stored at the referenced location to the
     * result of `<current value> [operator] [argument]`.
     */
    fun modify(operator: BinaryOperator, argument: ExpressionNode): UpdateNode
}

/** A reference to the value of a [MutableCell]. */
internal class CellReference(val name: ObjectVariableNode) : Reference {
    override fun get(): QueryNode {
        return QueryNode(
            name,
            QueryNameNode(Get, name.sourceLocation),
            Arguments(name.sourceLocation),
            name.sourceLocation
        )
    }

    override fun set(value: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            UpdateNameNode(Set, name.sourceLocation),
            Arguments.from(value),
            name.sourceLocation.merge(value.sourceLocation)
        )
    }

    override fun modify(operator: BinaryOperator, argument: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            UpdateNameNode(Modify(operator), name.sourceLocation),
            Arguments.from(argument),
            name.sourceLocation.merge(argument.sourceLocation)
        )
    }

    override val asDocument: Document
        get() = name.asDocument
}

/** A reference to a specific index of an [Vector]. */
internal class VectorReference(
    val name: ObjectVariableNode,
    val index: ExpressionNode,
    val sourceLocation: SourceLocation
) : Reference {
    override fun get(): QueryNode {
        return QueryNode(
            name,
            QueryNameNode(Get, name.sourceLocation),
            Arguments.from(index),
            sourceLocation
        )
    }

    override fun set(value: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            UpdateNameNode(Set, name.sourceLocation),
            Arguments.from(index, value),
            sourceLocation.merge(value.sourceLocation)
        )
    }

    override fun modify(operator: BinaryOperator, argument: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            UpdateNameNode(Modify(operator), name.sourceLocation),
            Arguments.from(index, argument),
            sourceLocation.merge(argument.sourceLocation)
        )
    }

    override val asDocument: Document
        get() = name + listOf(index).bracketed().nested()
}

/**
 * Returns a [Reference] to the location [queryNode] is reading.
 * The result is `null` if the query is not primitive.
 */
internal fun referenceFrom(queryNode: QueryNode): Reference? {
    return when {
        queryNode.query.value is Get && queryNode.arguments.isEmpty() ->
            CellReference(queryNode.variable)

        queryNode.query.value is Get && queryNode.arguments.size == 1 ->
            VectorReference(queryNode.variable, queryNode.arguments[0], queryNode.sourceLocation)

        else ->
            null
    }
}

/**
 * Returns a [Reference] to the location [updateNode] is writing.
 * The result is `null` if the update is not primitive.
 */
internal fun referenceFrom(updateNode: UpdateNode): Reference? {
    val updateIsPrimitive = updateNode.update.value is Set || updateNode.update.value is Modify
    return when {
        updateIsPrimitive && updateNode.arguments.size == 1 ->
            CellReference(updateNode.variable)

        updateIsPrimitive && updateNode.arguments.size == 2 ->
            VectorReference(updateNode.variable, updateNode.arguments[0], updateNode.sourceLocation)

        else ->
            null
    }
}
