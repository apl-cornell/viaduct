package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.subsequences

// Only select ABY for a selection if:
// for every simple statement that reads from the selection: //      the pc of that statement flows to pc of selection
//      if it's in a loop, the loop has a break
//      every break for that loop has a pc that flows to pc of selection

class ABYFactory(program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    private val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }
        hostSubsets.map(::ABY).map { SpecializedProtocol(it, hostTrustConfiguration) }
    }

    override fun protocols(): List<SpecializedProtocol> = protocols

    private fun LetNode.isApplicable(): Boolean {
        return nameAnalysis.readers(this).all { reader ->
            val pcCheck = informationFlowAnalysis.pcLabel(reader).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = nameAnalysis.involvedLoops(reader)
            val loopCheck = involvedLoops.all { loop ->
                nameAnalysis.correspondingBreaks(loop).isNotEmpty() && nameAnalysis.correspondingBreaks(loop).all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            true || pcCheck && loopCheck
        }
    }

    private fun DeclarationNode.isApplicable(): Boolean {
        return nameAnalysis.users(this).all { site ->
            val pcCheck = informationFlowAnalysis.pcLabel(site).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = nameAnalysis.involvedLoops(site)
            val loopCheck = involvedLoops.all { loop ->
                nameAnalysis.correspondingBreaks(loop).isNotEmpty() && nameAnalysis.correspondingBreaks(loop).all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            true || pcCheck && loopCheck
        }
    }

    private fun ObjectDeclarationArgumentNode.isApplicable(): Boolean {
        return nameAnalysis.users(this).all { site ->
            val pcCheck = informationFlowAnalysis.pcLabel(site).flowsTo(informationFlowAnalysis.pcLabel(this))
            val involvedLoops = nameAnalysis.involvedLoops(site)
            val loopCheck = involvedLoops.all { loop ->
                nameAnalysis.correspondingBreaks(loop).isNotEmpty() && nameAnalysis.correspondingBreaks(loop).all {
                    informationFlowAnalysis.pcLabel(it).flowsTo(informationFlowAnalysis.pcLabel(this))
                }
            }
            true || pcCheck && loopCheck
        }
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocols
            .filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }
            .map { it.protocol }
            .toSet()
}
