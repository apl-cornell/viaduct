package edu.cornell.cs.apl.viaduct.syntax

/** A built-in method. */
sealed class PrimitiveMethod : Method

// Queries

/** A built-in query. */
sealed class PrimitiveQuery : PrimitiveMethod(), Query

/** The get method for mutable cells. Reads the value stored in the cell. */
object CellGet : PrimitiveQuery() {
    override val arity: Int = 0
}

/** The get method for arrays. Reads the value stored at the given index. */
object ArrayGet : PrimitiveQuery() {
    override val arity: Int = 1
}

// Updates

/** A built-in update. */
sealed class PrimitiveUpdate : PrimitiveMethod(), Update

/** The set method for mutable cells. Replaces the value stored in the cell with a new value. */
object CellSet : PrimitiveUpdate() {
    override val arity: Int = 1
}

/** The set method for arrays. Replaces the value stored at the given index with a new value. */
object ArraySet : PrimitiveUpdate() {
    override val arity: Int = 2
}
