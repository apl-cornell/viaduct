package io.github.aplcornell.viaduct.security

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.aplcornell.viaduct.passes.PrincipalComponent
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.LabelVariable

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
