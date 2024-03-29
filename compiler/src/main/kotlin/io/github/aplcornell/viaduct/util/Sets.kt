package io.github.aplcornell.viaduct.util

/** Returns the union of all given sets. */
fun <T> Iterable<Set<T>>.unions(): Set<T> =
    this.fold(setOf()) { acc, s -> acc.union(s) }
