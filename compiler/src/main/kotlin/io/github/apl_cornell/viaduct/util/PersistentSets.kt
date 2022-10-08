package io.github.apl_cornell.viaduct.util

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/** Returns the union of all given sets. */
fun <T> Iterator<PersistentSet<T>>.unions(): PersistentSet<T> =
    this.asSequence().unions()

/** Returns the union of all given sets. */
fun <T> Iterable<PersistentSet<T>>.unions(): PersistentSet<T> =
    this.fold(persistentSetOf()) { acc, it -> acc.addAll(it) }

/** Returns the union of all given sets. */
fun <T> Sequence<PersistentSet<T>>.unions(): PersistentSet<T> =
    this.fold(persistentSetOf()) { acc, it -> acc.addAll(it) }
