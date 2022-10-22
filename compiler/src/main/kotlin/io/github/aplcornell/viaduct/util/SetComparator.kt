package io.github.aplcornell.viaduct.util

fun <T : Comparable<T>> Set<T>.asComparable() =
    object : Comparable<Set<T>> {
        // compare sets by creating lists from element ordering and
        // using lexicographic ordering
        override fun compareTo(other: Set<T>): Int {
            val selfOrdered: List<T> = (this@asComparable).toSortedSet().toList()
            val otherOrdered: List<T> = other.toSortedSet().toList()
            var i = 0

            while (selfOrdered.size < i || otherOrdered.size < i) {
                if (selfOrdered.size < i && otherOrdered.size >= i) {
                    return 1
                } else if (selfOrdered.size >= i && otherOrdered.size < i) {
                    return -1
                } else {
                    val currentHostOrder: Int = selfOrdered[i].compareTo(otherOrdered[i])

                    if (currentHostOrder != 0) {
                        return currentHostOrder
                    }

                    i++
                }
            }

            return 0
        }
    }
