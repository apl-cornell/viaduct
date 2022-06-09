package io.github.apl_cornell.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.passes.canMux
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.backends.cleartext.Local
import io.github.apl_cornell.viaduct.backends.cleartext.LocalProtocolFactory
import io.github.apl_cornell.viaduct.backends.cleartext.Replication
import io.github.apl_cornell.viaduct.backends.cleartext.ReplicationProtocolFactory
import io.github.apl_cornell.viaduct.selection.Literal
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.selection.SelectionConstraint
import io.github.apl_cornell.viaduct.selection.ands
import io.github.apl_cornell.viaduct.selection.readsFrom
import io.github.apl_cornell.viaduct.selection.sendsTo
import io.github.apl_cornell.viaduct.syntax.Operator
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.apl_cornell.viaduct.syntax.operators.Addition
import io.github.apl_cornell.viaduct.syntax.operators.And
import io.github.apl_cornell.viaduct.syntax.operators.EqualTo
import io.github.apl_cornell.viaduct.syntax.operators.LessThan
import io.github.apl_cornell.viaduct.syntax.operators.LessThanOrEqualTo
import io.github.apl_cornell.viaduct.syntax.operators.Multiplication
import io.github.apl_cornell.viaduct.syntax.operators.Mux
import io.github.apl_cornell.viaduct.syntax.operators.Not
import io.github.apl_cornell.viaduct.syntax.operators.Or
import io.github.apl_cornell.viaduct.util.subsequences

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
                    io.github.apl_cornell.viaduct.selection.And(
                        node.readsFrom(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                        node.sendsTo(nameAnalysis, setOf(it), localAndReplicated + setOf(it))
                    )
                }.ands()
            is DeclarationNode ->
                protocols.map {
                    io.github.apl_cornell.viaduct.selection.And(
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
