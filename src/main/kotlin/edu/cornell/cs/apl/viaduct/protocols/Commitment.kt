package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelector
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.specialize
import edu.cornell.cs.apl.viaduct.util.subsequences

class Commitment(val sender: Host, val recievers: Set<Host>) : Protocol {
    init {
        require(recievers.size >= 1)
        require(!recievers.contains(sender))
    }

    companion object {
        val protocolName = "Commitment"
    }

    override val hosts: Set<Host>
        get() = recievers.union(setOf(sender))

    override val name: String
        get() = protocolName

    override val protocolName: String
        get() = Commitment.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(sender) and (recievers.map { hostTrustConfiguration(it).integrity() }.reduce(Label::and))

    override fun equals(other: Any?): Boolean =
        other is Commitment && this.sender == other.sender && this.recievers == other.recievers

    override fun hashCode(): Int =
        hosts.hashCode()

    override val asDocument: Document
        get() = Document(protocolName)
}

class CommitmentSelector(
    val nameAnalysis: NameAnalysis,
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolSelector {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 1 }
    private val protocols: List<SpecializedProtocol> =
        hosts.flatMap { h ->
            hostSubsets.flatMap { s ->
                if (s.contains(h)) {
                    listOf()
                } else {
                    listOf(Commitment(h, s).specialize(hostTrustConfiguration))
                }
            }
        }

    private fun LetNode.isApplicable(): Boolean {
        return nameAnalysis.readers(this).all { reader ->
            reader.immediateRHS().all { e ->
                (e is AtomicExpressionNode) || (e is DowngradeNode)
            }
        }
    }

    override fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol> {
        if (node.isApplicable()) {
            return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
                .toSet()
        } else {
            return setOf()
        }
    }

    override fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol> {
        TODO("commitments for objects ")
    }
}
