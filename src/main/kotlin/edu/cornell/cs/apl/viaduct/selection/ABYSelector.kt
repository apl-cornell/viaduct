package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.uses
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.util.subsequences

// Only select ABY for a selection if:
// for every simple statement that reads from the selection: //      the pc of that statement flows to pc of selection
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
            val involvedLoops = nameAnalysis.involvedLoops(reader)
            val loopCheck = involvedLoops.all { loop ->
                nameAnalysis.correspondingBreaks(loop).isNotEmpty() && nameAnalysis.correspondingBreaks(loop).all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            pcCheck && loopCheck
        }
    }

    private fun DeclarationNode.isApplicable(): Boolean {
        return this.uses(nameAnalysis.tree).all { site ->
            val pcCheck = informationFlowAnalysis.pcLabel(site).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = nameAnalysis.involvedLoops(site)
            val loopCheck = involvedLoops.all { loop ->
                nameAnalysis.correspondingBreaks(loop).isNotEmpty() && nameAnalysis.correspondingBreaks(loop).all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            pcCheck && loopCheck
        }
    }

    override fun select(node: LetNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        if (node.isApplicable()) {
            return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
                .toSet()
        } else {
            return setOf()
        }
    }

    override fun select(node: DeclarationNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        if (node.isApplicable()) {
            return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
                .toSet()
        } else {
            return setOf()
        }
    }
}
