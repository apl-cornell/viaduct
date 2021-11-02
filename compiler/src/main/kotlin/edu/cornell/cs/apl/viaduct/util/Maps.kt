package edu.cornell.cs.apl.viaduct.util

/** Returns the union of all given maps. */
fun <K, V> Iterable<Map<K, V>>.unions(): Map<K, V> {
    val union = mutableMapOf<K, V>()
    this.forEach { union.putAll(it) }
    return union.toMap()
}
