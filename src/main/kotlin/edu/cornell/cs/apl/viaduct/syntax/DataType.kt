package edu.cornell.cs.apl.viaduct.syntax

/**
 * An object
 */
interface DataType {
    val constructor: Constructor

    val queries: Iterable<Query>

    val updates: Iterable<Update>

    val methods: Iterable<Method>
        get() = queries + updates
}
