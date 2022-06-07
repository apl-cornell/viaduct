package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.bracketed
import io.github.apl_cornell.apl.prettyprinting.nested
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.BinaryOperator
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.QueryNameNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.UpdateNameNode
import io.github.apl_cornell.viaduct.syntax.datatypes.Get
import io.github.apl_cornell.viaduct.syntax.datatypes.Modify
import io.github.apl_cornell.viaduct.syntax.datatypes.MutableCell
import io.github.apl_cornell.viaduct.syntax.datatypes.Set
import io.github.apl_cornell.viaduct.syntax.datatypes.Vector

/**
 * Represents an indexing into a container, such as a [MutableCell] or a [Vector].
 *
 * Any number of indices are allowed. For example, `x` (which is the same as `x[]`), `x[5]`,
 * and `x[1, 2, 3]` can all be represented with this class.
 *
 * This is used to unify reading from and writing to indices in container.
 */
internal class IndexingNode(
    private val variable: ObjectVariableNode,
    private val indices: Arguments<ExpressionNode>
) : Node() {
    /** Returns an expression that reads the value stored at the indexed location. */
    fun get(): QueryNode {
        return QueryNode(
            variable,
            QueryNameNode(Get, variable.sourceLocation),
            indices,
            this.sourceLocation
        )
    }

    /**
     * Returns a statement that changes the value stored at the indexed location to the
     * result of [value].
     */
    fun set(value: ExpressionNode, methodSourceLocation: SourceLocation): UpdateNode {
        return UpdateNode(
            variable,
            UpdateNameNode(Set, methodSourceLocation),
            Arguments(
                indices + value,
                sourceLocation = indices.sourceLocation.merge(value.sourceLocation)
            ),
            variable.sourceLocation.merge(value.sourceLocation)
        )
    }

    /**
     * Returns a statement that changes the value stored at the indexed location to the
     * result of `<current value> [operator] [argument]`.
     */
    fun modify(
        operator: BinaryOperator,
        argument: ExpressionNode,
        methodSourceLocation: SourceLocation
    ): UpdateNode {
        return UpdateNode(
            variable,
            UpdateNameNode(Modify(operator), methodSourceLocation),
            Arguments(
                indices + argument,
                sourceLocation = indices.sourceLocation.merge(argument.sourceLocation)
            ),
            variable.sourceLocation.merge(argument.sourceLocation)
        )
    }

    override val sourceLocation: SourceLocation
        get() = variable.sourceLocation.merge(indices.sourceLocation)

    override val comment: String?
        get() = null

    override fun toDocumentWithoutComment(): Document =
        if (indices.isEmpty())
            variable.toDocument()
        else
            variable + indices.bracketed().nested()

    companion object {
        /**
         * Returns the [IndexingNode] [queryNode] is reading.
         * The result is `null` if the query is not reading an index.
         */
        internal fun from(queryNode: QueryNode): IndexingNode? {
            return if (queryNode.query.value is Get)
                IndexingNode(queryNode.variable, queryNode.arguments)
            else
                null
        }

        /**
         * Returns the [IndexingNode] [updateNode] is writing.
         * The result is `null` if the update is not to an index.
         */
        internal fun from(updateNode: UpdateNode): IndexingNode? {
            val update = updateNode.update.value
            return if (update is Set || update is Modify) {
                val indices = Arguments(
                    updateNode.arguments.dropLast(1),
                    sourceLocation = updateNode.arguments.sourceLocation
                )
                IndexingNode(updateNode.variable, indices)
            } else {
                null
            }
        }
    }
}
