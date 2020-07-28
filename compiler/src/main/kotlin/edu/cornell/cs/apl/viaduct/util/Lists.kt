package edu.cornell.cs.apl.viaduct.util

/** Returns all subsequences of this list sorted from the smallest in size to the largest. */
internal fun <E> List<E>.subsequences(): Iterable<List<E>> {
    val result: MutableList<List<E>> = mutableListOf()
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
