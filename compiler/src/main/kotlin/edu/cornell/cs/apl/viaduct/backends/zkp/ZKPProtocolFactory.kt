package edu.cornell.cs.apl.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.Replication
import edu.cornell.cs.apl.viaduct.backends.cleartext.ReplicationProtocolFactory
import edu.cornell.cs.apl.viaduct.passes.canMux
import edu.cornell.cs.apl.viaduct.selection.Literal
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.SelectionConstraint
import edu.cornell.cs.apl.viaduct.selection.ands
import edu.cornell.cs.apl.viaduct.selection.readsFrom
import edu.cornell.cs.apl.viaduct.selection.sendsTo
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.VariableDeclarationNode
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

class ZKPProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)

    private val protocols: Set<Protocol> = run {
        val hostSubsets = program.hosts.sorted().subsequences().map { it.toSet() }
        hostSubsets.filter { it.size >= 2 }.flatMap { ss -> ss.map { h -> ZKP(h, ss - h) } }.toSet()
    }

    /** If the value is an op, ensure it's compatible with r1cs generation **/
    private fun ExpressionNode.compatibleOp(): Boolean =
        this !is OperatorApplicationNode || this.operator.isSupported()

    private fun Operator.isSupported(): Boolean =
        when (this) {
            is And, Not, Or, Multiplication, Addition, Mux, EqualTo, LessThan, LessThanOrEqualTo -> true
            else -> false
        }

    private fun VariableDeclarationNode.isApplicable(): Boolean =
        this !is LetNode || this.value.compatibleOp()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> =
        if (node.isApplicable()) protocols else setOf()

    private val localFactory = LocalProtocolFactory(program)
    private val replicationFactory = ReplicationProtocolFactory(program)
    private val localAndReplicated = localFactory.protocols + replicationFactory.protocols

    /** ZKP can only read from and send to itself, [Local], and [Replication]. **/
    override fun constraint(node: VariableDeclarationNode): SelectionConstraint =
        when (node) {
            // TODO: unify these cases
            is LetNode ->
                protocols.map {
                    edu.cornell.cs.apl.viaduct.selection.And(
                        node.readsFrom(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                        node.sendsTo(nameAnalysis, setOf(it), localAndReplicated + setOf(it))
                    )
                }.ands()
            is DeclarationNode ->
                protocols.map {
                    edu.cornell.cs.apl.viaduct.selection.And(
                        node.readsFrom(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                        node.sendsTo(nameAnalysis, setOf(it), localAndReplicated + setOf(it))
                    )
                }.ands()

            else -> {
                // TODO: is this correct for [ParameterNode]?
                super.constraint(node)
            }
        }

    override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
        when {
            protocol is ZKP && node.guard is ReadNode ->
                // Turn off visibility check when the conditional can be muxed.
                Literal(!node.canMux())
            else ->
                super.guardVisibilityConstraint(protocol, node)
        }
}
