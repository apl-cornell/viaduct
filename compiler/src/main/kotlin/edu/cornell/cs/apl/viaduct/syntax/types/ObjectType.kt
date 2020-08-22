package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName

/** The type of an object. */
abstract class ObjectType : Type {
    /** The class this object belongs to. */
    abstract val className: ClassName

    /** The type arguments [className] was instantiated with. */
    abstract val typeArguments: List<ValueType>

    /** The types of the arguments [className]'s constructor expects. */
    abstract val constructorArguments: List<ValueType>

    /**
     * Returns the type of [query] if this object has a query with that name,
     * and `null` otherwise.
     */
    abstract fun getType(query: QueryName): FunctionType?

    /**
     * Returns the type of [update] if this object has an update with that name,
     * and `null` otherwise.
     */
    abstract fun getType(update: UpdateName): FunctionType?

    final override val asDocument: Document
        get() = className + typeArguments.bracketed().nested()
}
