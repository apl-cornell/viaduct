package edu.cornell.cs.apl.viaduct.util

/**
 * Returns the list of all pairs where the first element is from [this] list,
 * and the second element is from [other].
 */
internal fun <A, B> List<A>.pairedWith(other: List<B>): List<Pair<A, B>> {
    val result = mutableListOf<Pair<A, B>>()
    for (a in this) {
        for (b in other) {
            result += Pair(a, b)
        }
    }
    return result
}

/** Returns all subsequences of this list sorted from shortest to longest. */
internal fun <E> List<E>.subsequences(): Iterable<List<E>> {
    val result = mutableListOf<List<E>>()
    for (i in 0..this.size) {
        result += this.subsequences(i)
    }
    return result
}

/** Returns all subsequences of this list of length [length]. */
// TODO: this can be sped up with dynamic programming.
private fun <E> List<E>.subsequences(length: Int): Iterable<List<E>> =
    when {
        length == 0 ->
            listOf(listOf())
        length == this.size ->
            listOf(this)
        length > this.size ->
            listOf()
        else ->
            this.drop(1).let {
                it.subsequences(length - 1).map { s -> listOf(this.first()) + s } +
                    it.subsequences(length)
            }
    }
