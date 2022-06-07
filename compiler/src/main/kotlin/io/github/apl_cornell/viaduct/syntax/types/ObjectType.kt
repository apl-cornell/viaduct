package io.github.apl_cornell.viaduct.syntax.types

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.bracketed
import io.github.apl_cornell.apl.prettyprinting.nested
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.datatypes.ClassName
import io.github.apl_cornell.viaduct.syntax.datatypes.QueryName
import io.github.apl_cornell.viaduct.syntax.datatypes.UpdateName

/** The type of objects. */
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

    final override fun toDocument(): Document = className + typeArguments.bracketed().nested()
}
