package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.aplcornell.viaduct.passes.PrincipalComponent
import io.github.aplcornell.viaduct.security.Component
import io.github.aplcornell.viaduct.security.ConfidentialityComponent
import io.github.aplcornell.viaduct.security.HostPrincipal
import io.github.aplcornell.viaduct.security.IntegrityComponent
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.Principal
import io.github.aplcornell.viaduct.security.actsFor
import io.github.aplcornell.viaduct.syntax.intermediate.AuthorityDelegationDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

/** A map that associates each host with its authority label. */
class HostTrustConfiguration internal constructor(val program: ProgramNode) : Analysis<ProgramNode> {
    val congruence: FreeDistributiveLatticeCongruence<Component<Principal>> =
        FreeDistributiveLatticeCongruence(
            program.filterIsInstance<AuthorityDelegationDeclarationNode>()
                .flatMap { it.congruences() } +
                program.filterIsInstance<HostDeclarationNode>().map {
                    FreeDistributiveLattice.LessThanOrEqualTo(
                        FreeDistributiveLattice(IntegrityComponent(HostPrincipal(it.name.value) as Principal) as PrincipalComponent),
                        FreeDistributiveLattice(ConfidentialityComponent(HostPrincipal(it.name.value) as Principal) as PrincipalComponent),
                    )
                },
        )

    fun actsFor(
        from: Label,
        to: Label,
    ) = actsFor(from, to, congruence)

    fun equals(
        from: Label,
        to: Label,
    ) = actsFor(from, to) && actsFor(to, from)
}
