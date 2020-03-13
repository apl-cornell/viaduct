package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.MPCWithAbort
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.subsequences

/**
 * A (very) primitive protocol selector that tries to assign [Local], [Replication], [MPCWithAbort]
 * to each [Variable] in that order. It does not use a cost model, or take into account syntactic
 * restrictions.
 */
fun ProcessDeclarationNode.selectProtocols(
    nameAnalysis: NameAnalysis,
    informationFlowAnalysis: InformationFlowAnalysis
): (Variable) -> Protocol {
    val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)

    val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }

    /** The list of all applicable protocols in the order we want to try them. */
    val protocols: List<SpecializedProtocol> =
        (hosts.map(::Local) + hostSubsets.map(::Replication) + hostSubsets.map(::MPCWithAbort))
            .map { SpecializedProtocol(it, hostTrustConfiguration) }

    /** First protocol in our protocol list that can secure [label]. */
    fun firstApplicable(label: Label): Protocol {
        protocols.firstOrNull { it.authority.actsFor(label) }.let {
            if (it == null) {
                // TODO: custom error class
                error("No protocol")
            } else {
                return it.protocol
            }
        }
    }

    // We need to use an object here for mutual recursion
    val protocolSelection = object {
        private val LetNode.protocol: Protocol by attribute {
            when (value) {
                is InputNode ->
                    Local(value.host.value)
                is QueryNode ->
                    nameAnalysis.declaration(value).protocol
                else ->
                    firstApplicable(informationFlowAnalysis.label(this))
            }
        }

        private val DeclarationNode.protocol: Protocol by attribute {
            firstApplicable(informationFlowAnalysis.label(this))
        }

        fun protocol(node: LetNode) = node.protocol

        fun protocol(node: DeclarationNode) = node.protocol
    }

    val protocolMap: Map<Variable, Protocol> =
        this.letNodes().associate { Pair(it.temporary.value, protocolSelection.protocol(it)) } +
            this.declarationNodes()
                .associate { Pair(it.variable.value, protocolSelection.protocol(it)) }

    return protocolMap::getValue
}

/** A [Protocol] specialized to a [HostTrustConfiguration] that caches [Protocol.authority]. */
private class SpecializedProtocol(
    val protocol: Protocol,
    hostTrustConfiguration: HostTrustConfiguration
) {
    val authority: Label = protocol.authority(hostTrustConfiguration)
}
