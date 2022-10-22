package io.github.apl_cornell.viaduct.runtime

/** Represents an output argument of a function call. */
class Out<T : Any> {
    private var value: T? = null

    fun set(value: T) {
        this.value = value
    }

    fun get(): T {
        return value!!
    }
}
