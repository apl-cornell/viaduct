package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.passes.canMux
import edu.cornell.cs.apl.viaduct.selection.FunctionVariable
import edu.cornell.cs.apl.viaduct.selection.Implies
import edu.cornell.cs.apl.viaduct.selection.Literal
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.SelectionConstraint
import edu.cornell.cs.apl.viaduct.selection.variableInSet
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
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

class ABYProtocolFactory(program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)

    // hack to get backpointer to parent factory
    var parentFactory: ProtocolFactory? = null
    var protocolComposer: ProtocolComposer? = null

    private val protocols: Set<Protocol> = run {
        val hosts = program.hosts.sorted()
        val hostPairs = hosts.pairedWith(hosts).filter { it.first < it.second }
        hostPairs.flatMap {
            listOf(ArithABY(it.first, it.second), BoolABY(it.first, it.second), YaoABY(it.first, it.second))
        }.toSet()
    }

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
        protocols.filter { node.isApplicable(it) }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> = protocols

    override fun viableProtocols(node: ParameterNode): Set<Protocol> = protocols

    private fun cleartextArrayLengthAndIndexConstraint(
        enclosingFunction: FunctionName,
        arrayObject: ObjectVariable,
        lengthOrIndexExpr: ReadNode
    ): SelectionConstraint {
        val exprDecl = nameAnalysis.declaration(lengthOrIndexExpr)
        val cleartextLengthProtocols =
            (parentFactory?.viableProtocols(exprDecl) ?: viableProtocols(exprDecl))
                .filter { lengthProtocol ->
                    if (protocols.all { protocolComposer!!.canCommunicate(lengthProtocol, it) }) {
                        protocols.all {
                            val events = protocolComposer!!.communicate(lengthProtocol, it)
                            events.all { event -> event.recv.id == ABY.CLEARTEXT_INPUT }
                        }
                    } else {
                        false
                    }
                }
                .toSet()

        return Implies(
            variableInSet(FunctionVariable(enclosingFunction, arrayObject), protocols),
            variableInSet(FunctionVariable(enclosingFunction, exprDecl.temporary.value), cleartextLengthProtocols)
        )
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
        when {
            protocol is ArithABY ->
                // Arithmetic circuits cannot mux, so keep the check.
                Literal(true)
            protocol is ABY && node.guard is ReadNode ->
                // Turn off visibility check when the conditional can be muxed.
                Literal(!node.canMux())
            else ->
                super.guardVisibilityConstraint(protocol, node)
        }
}