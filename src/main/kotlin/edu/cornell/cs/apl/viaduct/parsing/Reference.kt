package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.surface.Arguments
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode

/**
 * A pointer to a location such as the contents of a mutable cell or a specific index in an array.
 *
 * Used to unify reading from and writing to locations in the parser.
 */
internal interface Reference {
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
        return QueryNode(name, MutableCell.Get, Arguments(), name.sourceLocation)
    }

    override fun set(value: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            MutableCell.Set,
            Arguments(value),
            name.sourceLocation.merge(value.sourceLocation)
        )
    }

    override fun modify(operator: BinaryOperator, argument: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            MutableCell.Modify(operator),
            Arguments(argument),
            name.sourceLocation.merge(argument.sourceLocation)
        )
    }
}

/** A reference to a specific index of an [Vector]. */
internal class VectorReference(
    val name: ObjectVariableNode,
    val index: ExpressionNode,
    val sourceLocation: SourceLocation
) : Reference {
    override fun get(): QueryNode {
        return QueryNode(name, Vector.Get, Arguments(index), sourceLocation)
    }

    override fun set(value: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            Vector.Set,
            Arguments(index, value),
            sourceLocation.merge(value.sourceLocation)
        )
    }

    override fun modify(operator: BinaryOperator, argument: ExpressionNode): UpdateNode {
        return UpdateNode(
            name,
            Vector.Modify(operator),
            Arguments(index, argument),
            sourceLocation.merge(argument.sourceLocation)
        )
    }
}
