package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.apl_cornell.viaduct.passes.PrincipalComponent
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

fun Label.confidentiality(): Label = confidentiality(FreeDistributiveLattice.bounds())
fun Label.integrity(): Label = integrity(FreeDistributiveLattice.bounds())

fun flowsTo(from: Label, to: Label, congruence: FreeDistributiveLatticeCongruence<PrincipalComponent>) =
    congruence.lessThanOrEqualTo(to.confidentialityComponent, from.confidentialityComponent) &&
        congruence.lessThanOrEqualTo(from.integrityComponent, to.integrityComponent)

fun actsFor(from: Label, to: Label, congruence: FreeDistributiveLatticeCongruence<PrincipalComponent>) =
    congruence.lessThanOrEqualTo(from.confidentialityComponent, to.confidentialityComponent) &&
        congruence.lessThanOrEqualTo(from.integrityComponent, to.integrityComponent)
