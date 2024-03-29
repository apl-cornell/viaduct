package io.github.aplcornell.viaduct.syntax.datatypes

/** A write-only methods that modifies an object but returns no result. */
interface UpdateName : MethodName {
    override val nameCategory: String
        get() = "update"
}
