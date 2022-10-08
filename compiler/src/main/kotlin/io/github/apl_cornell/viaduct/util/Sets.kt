package io.github.apl_cornell.viaduct.util

/** Returns the union of all given sets. */
fun <T> Iterator<Set<T>>.unions(): Set<T> {
    val accumulator = mutableSetOf<T>()
    this.forEach { accumulator.addAll(it) }
    return accumulator
}

/** Returns the union of all given sets. */
fun <T> Iterable<Set<T>>.unions(): Set<T> =
    this.iterator().unions()

/** Returns the union of all given sets. */
fun <T> Sequence<Set<T>>.unions(): Set<T> =
    this.iterator().unions()
