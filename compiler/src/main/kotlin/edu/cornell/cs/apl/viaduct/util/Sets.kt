package edu.cornell.cs.apl.viaduct.util

/** Returns the union of all given sets. */
fun <T> Iterable<Set<T>>.unions(): Set<T> {
    return this.fold(setOf()) { acc, s -> acc.union(s) }
}
