package io.github.apl_cornell.viaduct.security

/*sealed class Component<A>(obj: A) {
    abstract val confidentialityComponent: Component<A>
    abstract val integrityComponent: Component<A>
}

data class PrincipalComponent(val principal: Principal) : Component<Principal>(principal) {
    override val confidentialityComponent: PrincipalComponent = PrincipalComponent(principal)
    override val integrityComponent: PrincipalComponent = PrincipalComponent(principal)
}*/

// data class ConfidentialityComponent<A>(val obj: A) : Component<A>(obj)

// data class IntegrityComponent<A>(val obj: A) : Component<A>(obj)

sealed class Component<A> {
    abstract val principal: A
    override fun toString(): String = principal.toString()
}

data class ConfidentialityComponent<A>(override val principal: A) : Component<A>()

data class IntegrityComponent<A>(override val principal: A) : Component<A>()

// sealed class PrincipalComponent(principal: Principal) : Component<Principal>(principal)

// data class ConfidentialityComponent(val principal: Principal) : PrincipalComponent(principal)

// data class IntegrityComponent(val principal: Principal) : PrincipalComponent(principal)
