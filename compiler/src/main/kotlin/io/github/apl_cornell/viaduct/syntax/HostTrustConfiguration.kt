package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.apl_cornell.viaduct.security.Component
import io.github.apl_cornell.viaduct.security.LabelComponent
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.intermediate.DelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode


/** A map that associates each host with its authority label. */
class HostTrustConfiguration(val program: ProgramNode) {
    val congruence: FreeDistributiveLatticeCongruence<Component<Principal>> =
        FreeDistributiveLatticeCongruence(
            program.filterIsInstance<DelegationDeclarationNode>()
                .flatMap { it ->
                    var node1Confidentiality: LabelComponent =
                        it.node1.value.interpret().confidentialityComponent
                    var node1Integrity: LabelComponent = it.node1.value.interpret().integrityComponent
                    var node2Confidentiality: LabelComponent =
                        it.node2.value.interpret().confidentialityComponent
                    var node2Integrity: LabelComponent = it.node2.value.interpret().integrityComponent

                    if (it.delegationKind == DelegationKind.IFC) {
                        node1Confidentiality = node2Confidentiality.also { node2Confidentiality = node1Confidentiality }
                    }
                    node1Confidentiality = node1Confidentiality.meet(node2Confidentiality)
                    node1Integrity = node1Integrity.meet(node2Integrity)

                    when (it.delegationProjection) {
                        DelegationProjection.CONFIDENTIALITY ->
                            listOf(Pair(node1Confidentiality, node2Confidentiality))

                        DelegationProjection.INTEGRITY ->
                            listOf(Pair(node1Integrity, node2Integrity))

                        DelegationProjection.BOTH ->
                            listOf(
                                Pair(node1Confidentiality, node2Confidentiality),
                                Pair(node1Integrity, node2Integrity)
                            )
                    }
                })
}
