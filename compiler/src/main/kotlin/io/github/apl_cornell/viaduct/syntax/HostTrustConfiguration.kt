package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.apl_cornell.viaduct.analysis.AnalysisProvider
import io.github.apl_cornell.viaduct.passes.PrincipalComponent
import io.github.apl_cornell.viaduct.security.Component
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.HostPrincipal
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.intermediate.AuthorityDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

/** A map that associates each host with its authority label. */
class HostTrustConfiguration private constructor(val program: ProgramNode?) {

    val congruence: FreeDistributiveLatticeCongruence<Component<Principal>> =
        if (program != null)
            FreeDistributiveLatticeCongruence(
                program.filterIsInstance<AuthorityDelegationDeclarationNode>()
                    .flatMap { it.congruences() } +
                    program.filterIsInstance<HostDeclarationNode>().map {
                        Pair(
                            FreeDistributiveLattice(IntegrityComponent(HostPrincipal(it.name.value) as Principal) as PrincipalComponent),
                            FreeDistributiveLattice(ConfidentialityComponent(HostPrincipal(it.name.value) as Principal) as PrincipalComponent)
                        )
                    }
            )
        else
            FreeDistributiveLatticeCongruence.empty()

    fun actsFor(from: Label, to: Label) =
        congruence.lessThanOrEqualTo(from.confidentialityComponent, to.confidentialityComponent) &&
            congruence.lessThanOrEqualTo(from.integrityComponent, to.integrityComponent)

    fun equals(from: Label, to: Label) =
        actsFor(from, to) && actsFor(to, from)

    companion object : AnalysisProvider<HostTrustConfiguration> {
        private fun construct(program: ProgramNode) =
            HostTrustConfiguration(program)

        override fun get(program: ProgramNode): HostTrustConfiguration = program.cached(::construct)

        val empty: HostTrustConfiguration = HostTrustConfiguration(null)
    }
}
