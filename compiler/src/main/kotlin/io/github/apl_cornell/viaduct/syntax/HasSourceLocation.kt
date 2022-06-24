package io.github.apl_cornell.viaduct.syntax

/** A node in the abstract syntax tree that has a [SourceLocation]. */
interface HasSourceLocation {
    val sourceLocation: SourceLocation
}
