package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.Constructor
import edu.cornell.cs.apl.viaduct.syntax.DataType
import edu.cornell.cs.apl.viaduct.syntax.Query
import edu.cornell.cs.apl.viaduct.syntax.Update

/** An object containing a single modifiable value. */
object MutableCell : DataType {
    override val constructor: Constructor = object :
        Constructor {}

    /** Returns the value stored in the cell. */
    object Get : Query {
        override val arity: Int
            get() = 0
    }

    /** Replaces the value stored in the cell with a new value. */
    object Set : Update {
        override val arity: Int
            get() = 1
    }

    /**
     * Applies a binary operator to the current value and the argument, and sets the stored value
     * to the result.
     */
    data class Modify(val operator: BinaryOperator) : Update {
        override val arity: Int
            get() = 1
    }

    override val queries: Iterable<Query>
        get() = listOf(Get)

    override val updates: Iterable<Update>
        get() = listOf(Set)
}
