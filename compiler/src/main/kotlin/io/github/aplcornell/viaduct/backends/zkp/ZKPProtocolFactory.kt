package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.backends.cleartext.LocalProtocolFactory
import io.github.aplcornell.viaduct.backends.cleartext.Replication
import io.github.aplcornell.viaduct.backends.cleartext.ReplicationProtocolFactory
import io.github.aplcornell.viaduct.passes.canMux
import io.github.aplcornell.viaduct.selection.Literal
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.selection.SelectionConstraint
import io.github.aplcornell.viaduct.selection.ands
import io.github.aplcornell.viaduct.selection.readsFrom
import io.github.aplcornell.viaduct.selection.sendsTo
import io.github.aplcornell.viaduct.syntax.Operator
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.operators.Addition
import io.github.aplcornell.viaduct.syntax.operators.And
import io.github.aplcornell.viaduct.syntax.operators.EqualTo
import io.github.aplcornell.viaduct.syntax.operators.LessThan
import io.github.aplcornell.viaduct.syntax.operators.LessThanOrEqualTo
import io.github.aplcornell.viaduct.syntax.operators.Multiplication
import io.github.aplcornell.viaduct.syntax.operators.Mux
import io.github.aplcornell.viaduct.syntax.operators.Not
import io.github.aplcornell.viaduct.syntax.operators.Or
import io.github.aplcornell.viaduct.util.subsequences

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
                    io.github.aplcornell.viaduct.selection.And(
                        node.readsFrom(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                        node.sendsTo(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                    )
                }.ands()

            is DeclarationNode ->
                protocols.map {
                    io.github.aplcornell.viaduct.selection.And(
                        node.readsFrom(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
                        node.sendsTo(nameAnalysis, setOf(it), localAndReplicated + setOf(it)),
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
