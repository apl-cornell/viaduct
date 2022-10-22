package io.github.apl_cornell.viaduct.util

/** Returns the union of all given sets. */
fun <T> Iterable<Set<T>>.unions(): Set<T> =
    this.fold(setOf()) { acc, s -> acc.union(s) }
