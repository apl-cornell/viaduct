package io.github.aplcornell.viaduct.backends.aby

import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.passes.canMux
import io.github.aplcornell.viaduct.selection.FunctionVariable
import io.github.aplcornell.viaduct.selection.Implies
import io.github.aplcornell.viaduct.selection.Literal
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.selection.SelectionConstraint
import io.github.aplcornell.viaduct.selection.variableInSet
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.operators.ComparisonOperator
import io.github.aplcornell.viaduct.syntax.operators.Division
import io.github.aplcornell.viaduct.syntax.operators.LogicalOperator
import io.github.aplcornell.viaduct.syntax.operators.Maximum
import io.github.aplcornell.viaduct.syntax.operators.Minimum
import io.github.aplcornell.viaduct.syntax.operators.Mux
import io.github.aplcornell.viaduct.util.pairedWith

// Only select ABY for a selection if:
// for every simple statement that reads from the selection:
//      the pc of that statement flows to pc of selection
//      if it's in a loop, the loop has a break
//      every break for that loop has a pc that flows to pc of selection

class ABYProtocolFactory(program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = program.analyses.get<NameAnalysis>()

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
                        io.github.aplcornell.viaduct.syntax.operators.Not,
                        Mux, Maximum, Minimum, Division,
                        ->
                            protocol !is ArithABY

                        else -> true
                    }

                else -> true
            }

        return operationCheck
    }

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> =
        when (node) {
            is LetNode ->
                protocols.filter { node.isApplicable(it) }.toSet()

            else ->
                protocols
        }

    private fun cleartextArrayLengthAndIndexConstraint(
        enclosingFunction: FunctionName,
        arrayObject: ObjectVariable,
        lengthOrIndexExpr: ReadNode,
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
            variableInSet(FunctionVariable(enclosingFunction, exprDecl.name.value), cleartextLengthProtocols),
        )
    }

    override fun constraint(node: VariableDeclarationNode): SelectionConstraint =
        when {
            node is LetNode && node.value is QueryNode -> {
                val rhs = node.value
                val objectType = nameAnalysis.objectType(nameAnalysis.declaration(rhs))
                if (objectType.className.value == Vector && rhs.query.value == Get && rhs.arguments[0] is ReadNode) {
                    cleartextArrayLengthAndIndexConstraint(
                        nameAnalysis.enclosingFunctionName(node),
                        rhs.variable.value,
                        rhs.arguments[0] as ReadNode,
                    )
                } else {
                    super.constraint(node)
                }
            }

            node is DeclarationNode && node.objectType.className.value == Vector && node.arguments[0] is ReadNode ->
                cleartextArrayLengthAndIndexConstraint(
                    nameAnalysis.enclosingFunctionName(node),
                    node.name.value,
                    node.arguments[0] as ReadNode,
                )

            else ->
                super.constraint(node)
        }

    override fun constraint(node: UpdateNode): SelectionConstraint {
        val objectType = nameAnalysis.objectType(nameAnalysis.declaration(node))
        return if (objectType.className.value == Vector &&
            node.update.value == io.github.aplcornell.viaduct.syntax.datatypes.Set &&
            node.arguments[0] is ReadNode
        ) {
            cleartextArrayLengthAndIndexConstraint(
                nameAnalysis.enclosingFunctionName(node),
                node.variable.value,
                node.arguments[0] as ReadNode,
            )
        } else {
            super.constraint(node)
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
