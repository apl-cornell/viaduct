package edu.cornell.cs.apl.viaduct.runtime

abstract class Boxed {
    abstract var temporary: Any

    fun set(update: Any) {
        temporary = update
    }

    fun get(): Any {
        return temporary
    }
}
