package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.backend.aby.canMux
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
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

    val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostPairs = hosts.pairedWith(hosts).filter { it.first < it.second }
        hostPairs.flatMap {
            listOf(
                SpecializedProtocol(ArithABY(it.first, it.second), hostTrustConfiguration),
                SpecializedProtocol(BoolABY(it.first, it.second), hostTrustConfiguration),
                SpecializedProtocol(YaoABY(it.first, it.second), hostTrustConfiguration)
            )
        }
    }

    override fun protocols(): List<SpecializedProtocol> = protocols

    override fun availableProtocols(): Set<ProtocolName> =
        setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    private fun LetNode.isApplicable(protocol: Protocol): Boolean {
        val readerCheck =
            nameAnalysis.readers(this).all { reader ->
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

        val operationCheck =
            when (val rhs = this.value) {
                is OperatorApplicationNode ->
                    when (rhs.operator) {
                        is ComparisonOperator, is LogicalOperator, Mux, Maximum, Minimum, Division ->
                            protocol !is ArithABY

                        else -> true
                    }

                else -> true
            }

        // TODO: add check---if index is secret, array cannot be stored
        // in ArithABY because it doesn't support muxing
        // need this in array queries and updates

        return readerCheck && operationCheck
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        protocols.map { it.protocol }.filter { node.isApplicable(it) }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

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
