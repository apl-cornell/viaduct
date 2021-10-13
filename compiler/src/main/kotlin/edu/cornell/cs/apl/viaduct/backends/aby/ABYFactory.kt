package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.passes.canMux
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.selection.FunctionVariable
import edu.cornell.cs.apl.viaduct.selection.Implies
import edu.cornell.cs.apl.viaduct.selection.Literal
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.SelectionConstraint
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.VariableIn
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.ComparisonOperator
import edu.cornell.cs.apl.viaduct.syntax.operators.Division
import edu.cornell.cs.apl.viaduct.syntax.operators.LogicalOperator
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.util.pairedWith

// Only select ABY for a selection if:
// for every simple statement that reads from the selection:
//      the pc of that statement flows to pc of selection
//      if it's in a loop, the loop has a break
//      every break for that loop has a pc that flows to pc of selection

class ABYFactory(program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)

    // hack to get backpointer to parent factory
    var parentFactory: ProtocolFactory? = null

    val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostPairs = hosts.pairedWith(hosts).filter { it.first < it.second }
        hostPairs.flatMap {
            // ABY is secure only in semi-honest,
            // so the integrity of one should imply the integrity of the other
            val h1Label: Label = hostTrustConfiguration[it.first]!!.interpret()
            val h2Label: Label = hostTrustConfiguration[it.second]!!.interpret()
            val combinedConfidentiality = h1Label.confidentiality().and(h1Label.confidentiality())
            val semihonest =
                h1Label.integrity().swap().actsFor(combinedConfidentiality) &&
                    h2Label.integrity().swap().actsFor(combinedConfidentiality)
            if (semihonest) {
                listOf(
                    SpecializedProtocol(ArithABY(it.first, it.second), hostTrustConfiguration),
                    SpecializedProtocol(BoolABY(it.first, it.second), hostTrustConfiguration),
                    SpecializedProtocol(YaoABY(it.first, it.second), hostTrustConfiguration)
                )
            } else {
                listOf()
            }
        }
    }

    override fun protocols(): List<SpecializedProtocol> = protocols

    override fun availableProtocols(): Set<ProtocolName> =
        setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    private fun LetNode.isApplicable(protocol: Protocol): Boolean {
        val operationCheck =
            when (val rhs = this.value) {
                is OperatorApplicationNode ->
                    when (rhs.operator) {
                        is ComparisonOperator, is LogicalOperator,
                        edu.cornell.cs.apl.viaduct.syntax.operators.Not,
                        Mux, Maximum, Minimum, Division ->
                            protocol !is ArithABY

                        else -> true
                    }

                else -> true
            }

        return operationCheck
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        protocols.map { it.protocol }.filter { node.isApplicable(it) }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

    private fun cleartextArrayLengthAndIndexConstraint(
        enclosingFunction: FunctionName,
        arrayObject: ObjectVariable,
        lengthOrIndexExpr: ReadNode
    ): SelectionConstraint {
        val exprDecl = nameAnalysis.declaration(lengthOrIndexExpr)
        val mpcProtocols = protocols.map { it.protocol }.toSet()
        val cleartextLengthProtocols =
            (parentFactory?.viableProtocols(exprDecl) ?: viableProtocols(exprDecl))
                .filter { lengthProtocol ->
                    if (mpcProtocols.all { SimpleProtocolComposer.canCommunicate(lengthProtocol, it) }) {
                        mpcProtocols.all {
                            val events = SimpleProtocolComposer.communicate(lengthProtocol, it)
                            events.all { event -> event.recv.id == ABY.CLEARTEXT_INPUT }
                        }
                    } else {
                        false
                    }
                }
                .toSet()

        return Implies(
            VariableIn(FunctionVariable(enclosingFunction, arrayObject), mpcProtocols),
            VariableIn(FunctionVariable(enclosingFunction, exprDecl.temporary.value), cleartextLengthProtocols)
        )
    }

    override fun constraint(node: DeclarationNode): SelectionConstraint {
        return if (node.className.value == Vector && node.arguments[0] is ReadNode) {
            cleartextArrayLengthAndIndexConstraint(
                nameAnalysis.enclosingFunctionName(node),
                node.name.value,
                node.arguments[0] as ReadNode
            )
        } else {
            Literal(true)
        }
    }

    override fun constraint(node: LetNode): SelectionConstraint =
        when (val rhs = node.value) {
            is QueryNode -> {
                val objectDecl = nameAnalysis.declaration(rhs)
                if (objectDecl.className.value == Vector && rhs.query.value == Get && rhs.arguments[0] is ReadNode) {
                    cleartextArrayLengthAndIndexConstraint(
                        nameAnalysis.enclosingFunctionName(node),
                        rhs.variable.value,
                        rhs.arguments[0] as ReadNode
                    )
                } else {
                    Literal(true)
                }
            }

            else -> Literal(true)
        }

    override fun constraint(node: UpdateNode): SelectionConstraint {
        val objectDecl = nameAnalysis.declaration(node)
        return if (objectDecl.className.value == Vector &&
            node.update.value == edu.cornell.cs.apl.viaduct.syntax.datatypes.Set &&
            node.arguments[0] is ReadNode
        ) {
            cleartextArrayLengthAndIndexConstraint(
                nameAnalysis.enclosingFunctionName(node),
                node.variable.value,
                node.arguments[0] as ReadNode
            )
        } else {
            Literal(true)
        }
    }

    override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
        when (node.guard) {
            is LiteralNode -> Literal(true)

            // turn off visibility check when the conditional can be muxed
            is ReadNode -> {
                // arith circuit cannot mux, so keep the check then
                if (protocol is ArithABY) {
                    Literal(true)
                } else {
                    Literal(!node.canMux())
                }
            }
        }
}
