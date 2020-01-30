package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.Constructor
import edu.cornell.cs.apl.viaduct.syntax.DataType
import edu.cornell.cs.apl.viaduct.syntax.Query
import edu.cornell.cs.apl.viaduct.syntax.Update

/** An array of mutable cells. */
object Vector : DataType {
    override val constructor: Constructor = object :
        Constructor {}

    /** Returns the value stored at the given index. */
    object Get : Query {
        override val arity: Int
            get() = 1
    }

    /** Replaces the value stored at the given index with a new value. */
    object Set : Update {
        override val arity: Int
            get() = 2
    }

    /**
     * Applies a binary operator to the value at the given index and the argument,
     * and sets the value stored at that index to the result.
     */
    data class Modify(val operator: BinaryOperator) : Update {
        override val arity: Int
            get() = 2
    }

    override val queries: Iterable<Query>
        get() = listOf(Get)

    override val updates: Iterable<Update>
        get() = listOf(Set)
}
