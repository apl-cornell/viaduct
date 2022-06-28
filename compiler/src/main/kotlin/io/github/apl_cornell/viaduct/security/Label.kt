package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable

typealias Label = SecurityLattice<FreeDistributiveLattice<Component<Principal>>>

val Host.label
    get() =
        Label(
            FreeDistributiveLattice(ConfidentialityComponent(HostPrincipal(this))),
            FreeDistributiveLattice(IntegrityComponent(HostPrincipal(this)))
        )

val LabelVariable.label
    get() =
        Label(
            FreeDistributiveLattice(ConfidentialityComponent(PolymorphicPrincipal(this))),
            FreeDistributiveLattice(IntegrityComponent(PolymorphicPrincipal(this)))
        )
