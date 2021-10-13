package edu.cornell.cs.apl.viaduct.backends.zkp

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.ReplicationFactory
import edu.cornell.cs.apl.viaduct.passes.canMux
import edu.cornell.cs.apl.viaduct.selection.Literal
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.SelectionConstraint
import edu.cornell.cs.apl.viaduct.selection.ands
import edu.cornell.cs.apl.viaduct.selection.readsFrom
import edu.cornell.cs.apl.viaduct.selection.sendsTo
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import edu.cornell.cs.apl.viaduct.util.subsequences

class ZKPFactory(val program: ProgramNode) : ProtocolFactory {
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
            }.map { SpecializedProtocol(ZKP(it.first, it.second), hostTrustConfiguration) }
        }

        fun protocols(program: ProgramNode): List<SpecializedProtocol> = program.instance
    }

    override fun protocols(): List<SpecializedProtocol> = protocols(program)

    /** If the value is an op, ensure it's compatible with r1cs generation **/
    private fun ExpressionNode.compatibleOp(): Boolean {
        return if (this is OperatorApplicationNode) {
            (this.operator.isSupported())
        } else {
            true
        }
    }

    private fun Operator.isSupported(): Boolean =
        when (this) {
            is And, Not, Or, Multiplication, Addition, Mux, EqualTo, LessThan, LessThanOrEqualTo -> true
            else -> false
        }

    private fun Node.isApplicable(): Boolean =
        when (this) {
            is LetNode -> this.value.compatibleOp()
            is DeclarationNode -> true
            is ObjectDeclarationArgumentNode -> true
            is ParameterNode -> true
            else -> throw Exception("How did I get here?")
        }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun availableProtocols(): Set<ProtocolName> = setOf(ZKP.protocolName)

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    private val localFactory = LocalFactory(program)
    private val replicationFactory = ReplicationFactory(program)

    private val localAndReplicated: Set<Protocol> =
        localFactory.protocols.map { it.protocol }.toSet() +
            replicationFactory.protocols.map { it.protocol }.toSet()

    /** ZKP can only read from, and only send to, itself, local, and replicated **/
    override fun constraint(node: LetNode): SelectionConstraint =
        protocols(program).map {
            edu.cornell.cs.apl.viaduct.selection.And(
                node.readsFrom(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)),
                node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol))
            )
        }.ands()

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        protocols(program).map {
            edu.cornell.cs.apl.viaduct.selection.And(
                node.readsFrom(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)),
                node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol))
            )
        }.ands()

    override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
        when (node.guard) {
            is LiteralNode -> Literal(true)

            // turn off visibility check when the conditional can be muxed
            is ReadNode -> Literal(!node.canMux())
        }
}
