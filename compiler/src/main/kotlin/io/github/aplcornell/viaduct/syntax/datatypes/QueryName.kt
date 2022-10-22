package io.github.apl_cornell.viaduct.syntax.datatypes

/** A read-only method that returns information about the object without modifying it. */
interface QueryName : MethodName {
    override val nameCategory: String
        get() = "query"
}
