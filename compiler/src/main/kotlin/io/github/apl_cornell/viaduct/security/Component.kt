package io.github.apl_cornell.viaduct.security

sealed class Component<A> {
    abstract val principal: A
}

data class ConfidentialityComponent<A>(override val principal: A) : Component<A>() {
    override fun toString(): String = principal.toString()
}

data class IntegrityComponent<A>(override val principal: A) : Component<A>() {
    override fun toString(): String = principal.toString()
}
