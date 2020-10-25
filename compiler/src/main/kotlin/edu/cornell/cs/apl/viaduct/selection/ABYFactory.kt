package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.backend.aby.canMux
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.util.pairedWith

// Only select ABY for a selection if:
// for every simple statement that reads from the selection:
//      the pc of that statement flows to pc of selection
//      if it's in a loop, the loop has a break
//      every break for that loop has a pc that flows to pc of selection

class ABYFactory(program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostPairs = hosts.pairedWith(hosts).filter { it.first < it.second }
        hostPairs.map { SpecializedProtocol(ABY(it.first, it.second), hostTrustConfiguration) }
    }

    override fun protocols(): List<SpecializedProtocol> = protocols

    override fun availableProtocols(): Set<ProtocolName> = setOf(ABY.protocolName)

    private fun LetNode.isApplicable(): Boolean {
        return nameAnalysis.readers(this).all { reader ->
            // array length can't be in MPC
            val arrayLengthCheck =
                when (reader) {
                    is DeclarationNode ->
                        when (reader.className.value) {
                            Vector ->
                                when (val index = reader.arguments[0]) {
                                    is ReadNode -> index.temporary.value != this.temporary.value
                                    else -> true
                                }

                            else -> true
                        }

                    else -> true
                }

            arrayLengthCheck
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

    override fun constraint(node: DeclarationNode): SelectionConstraint {
        return Literal(true)
        /*
        return if (node.className.value == Vector && node.arguments[0] is ReadNode) {
            val lengthExpr = node.arguments[0] as ReadNode
            val lengthExprDecl = nameAnalysis.declaration(lengthExpr)
            val enclosingFunction = nameAnalysis.enclosingFunctionName(lengthExprDecl)
            val mpcProtocols = protocols.map { it.protocol }.toSet()

            Implies(
                VariableIn(FunctionVariable(enclosingFunction, node.name.value), mpcProtocols),
                Not(VariableIn(FunctionVariable(enclosingFunction, lengthExprDecl), mpcProtocols))
            )
        } else {
            Literal(true)
        }
        */
    }

    override fun constraint(node: LetNode): SelectionConstraint {
        return super.constraint(node)
    }

    override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
        when (node.guard) {
            is LiteralNode -> Literal(true)

            // turn off visibility check when the conditional can be muxed
            is ReadNode -> Literal(!node.canMux())
        }
}
