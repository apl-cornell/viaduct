package io.github.apl_cornell.viaduct.runtime

/** Represents a mutable cell. */
class Boxed<T>(private var value: T) {

    fun set(update: T) {
        value = update
    }

    fun get(): T {
        return value
    }
}
