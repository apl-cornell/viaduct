package edu.cornell.cs.apl.viaduct.syntax

/** A node in the abstract syntax tree that has a [SourceLocation]. */
interface HasSourceLocation {
    val sourceLocation: SourceLocation
}
