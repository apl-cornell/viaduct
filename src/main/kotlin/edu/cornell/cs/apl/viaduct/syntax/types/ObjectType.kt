package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName

interface ObjectType {
    /** The types of the arguments the constructor of this object takes. */
    val constructorArguments: List<ValueType>

    /**
     * Returns the type of [query] if this object has a query with that name,
     * and `null` otherwise.
     */
    fun getType(query: QueryName): FunctionType?

    /**
     * Returns the type of [update] if this object has an update with that name,
     * and `null` otherwise.
     */
    fun getType(update: UpdateName): FunctionType?
}
