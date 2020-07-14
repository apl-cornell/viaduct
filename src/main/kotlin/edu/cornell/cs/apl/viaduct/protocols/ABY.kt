package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.correspondingBreaks
import edu.cornell.cs.apl.viaduct.analysis.involvedLoops
import edu.cornell.cs.apl.viaduct.analysis.uses
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelector
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.util.subsequences

/**
 * An MPC protocol that provides security against a dishonest majority.
 * More specifically, the protocol should preserve confidentiality and integrity when up to
 * n - 1 out of the n participating hosts are corrupted.
 * In return, availability may be lost even with a single corrupted participant.
 */
class ABY(hosts: Set<Host>) : MPCProtocol, SymmetricProtocol(hosts) {
    init {
        require(hosts.size >= 2)
    }

    companion object {
        val protocolName = "MPCWithAbort"
    }

    override val protocolName: String
        get() = ABY.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::and)

    override fun equals(other: Any?): Boolean =
        other is ABY && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()
}

// Only select ABY for a selection if:
// for every simple statement that reads from the selection:
//      the pc of that statement flows to pc of selection
//      if it's in a loop, the loop has a break
//      every break for that loop has a pc that flows to pc of selection

class ABYSelector(
    val nameAnalysis: NameAnalysis,
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolSelector {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }
    private val protocols: List<SpecializedProtocol> =
        hostSubsets.map(::ABY).map { SpecializedProtocol(it, hostTrustConfiguration) }

    private fun LetNode.isApplicable(): Boolean {
        return nameAnalysis.readers(this).all { reader ->
            val pcCheck = informationFlowAnalysis.pcLabel(reader).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = reader.involvedLoops(nameAnalysis.tree)
            val loopCheck = involvedLoops.all { loop ->
                loop.correspondingBreaks().isNotEmpty() && loop.correspondingBreaks().all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            pcCheck && loopCheck
        }
    }

    private fun DeclarationNode.isApplicable(): Boolean {
        return this.uses(nameAnalysis.tree).all { site ->
            val pcCheck = informationFlowAnalysis.pcLabel(site).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = site.involvedLoops(nameAnalysis.tree)
            val loopCheck = involvedLoops.all { loop ->
                loop.correspondingBreaks().isNotEmpty() && loop.correspondingBreaks().all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            pcCheck && loopCheck
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
        if (node.isApplicable()) {
            return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
                .toSet()
        } else {
            return setOf()
        }
    }
}
