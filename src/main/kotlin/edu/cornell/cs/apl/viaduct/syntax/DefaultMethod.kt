package edu.cornell.cs.apl.viaduct.syntax

sealed class DefaultMethod : Method

sealed class DefaultQuery : DefaultMethod(), Query

object RegisterRead : DefaultQuery() {
    override val arity: Int = 0
}

object ArrayRead : DefaultQuery() {
    override val arity: Int = 1
}

sealed class DefaultUpdate : DefaultMethod(), Update

object RegisterWrite : DefaultUpdate() {
    override val arity: Int = 1
}

object ArrayWrite : DefaultUpdate() {
    override val arity: Int = 2
}

