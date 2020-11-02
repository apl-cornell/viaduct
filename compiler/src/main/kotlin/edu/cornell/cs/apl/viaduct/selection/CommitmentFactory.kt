package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class CommitmentFactory(val program: ProgramNode) : ProtocolFactory {

    private val nameAnalysis = NameAnalysis.get(program)

    companion object {
        private val ProgramNode.instance: List<SpecializedProtocol> by attribute {
            val hostTrustConfiguration = HostTrustConfiguration(this)
            val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
            val hostSubsets = hosts.subsequences().map { it.toSet() }
            hostSubsets.filter { it.size >= 2 }.flatMap { ss ->
                ss.map { h ->
                    (h to ss.minus(h))
                }
            }.map { SpecializedProtocol(Commitment(it.first, it.second), hostTrustConfiguration) }
        }

        fun protocols(program: ProgramNode): List<SpecializedProtocol> = program.instance
    }

    override fun protocols(): List<SpecializedProtocol> = protocols(program)

    override fun availableProtocols(): Set<ProtocolName> = setOf(Commitment.protocolName)

    private fun Node.isApplicable(): Boolean {
        return when (this) {
            is LetNode -> nameAnalysis.readers(this).all {
                it.immediateRHS().all {
                    (it is AtomicExpressionNode) || (it is DowngradeNode)
                }
            } && ((this.value is AtomicExpressionNode) || (this.value is DowngradeNode) || (this.value is QueryNode))
            is DeclarationNode -> nameAnalysis.updaters(this).isEmpty()
            else -> false
        }
    }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }
    }

    override fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }
    }

    override fun viableProtocols(node: ParameterNode): Set<Protocol> {
        return protocols(program).map { it.protocol }.toSet()
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }
    }

    private val localFactory = LocalFactory(program)
    private val replicationFactory = ReplicationFactory(program)

    private val localAndReplicated: Set<Protocol> =
        localFactory.protocols.map { it.protocol }.toSet() +
            replicationFactory.protocols.map { it.protocol }.toSet()

    /** Commitment can only send to itself, local, and replicated **/

    override fun constraint(node: LetNode): SelectionConstraint =
        protocols(program).map {
            node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol))
        }.ands()

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        protocols(program).map {
            And(
                node.readsFrom(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)),
                node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol))
            )
        }.ands()
}
