package io.github.apl_cornell.viaduct.runtime

/** Represents a mutable cell. */
class Boxed<T>(var value: T) {

    fun set(update: T) {
        value = update
    }

    fun get(): T {
        return value
    }
}
