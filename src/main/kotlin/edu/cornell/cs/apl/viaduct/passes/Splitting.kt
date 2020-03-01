package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TemporaryDefinition

/**
 * Returns a map from [Protocol]s to the portions of [this] process assigned to them.
 *
 * The result contains an entry for each [Protocol] involved in the execution of [this] process.
 * The value mapped to each [Protocol] is the body of [this] process with parts irrelevant to
 * that [Protocol] erased.
 */
fun ProcessDeclarationNode.split(
    protocolAnalysis: ProtocolAnalysis,
    typeAnalysis: TypeAnalysis
): Map<Protocol, BlockNode> {
    fun BlockNode.projectFor(protocol: Protocol): BlockNode {
        val statements: List<StatementNode> = this.statements.flatMap {
            if (protocol !in protocolAnalysis.protocols(it))
                listOf()
            else when (it) {
                is SimpleStatementNode -> {
                    val result = mutableListOf<StatementNode>()
                    val primaryProtocol = protocolAnalysis.primaryProtocol(it)

                    if (protocol == primaryProtocol)
                        result.add(it)

                    if (it is TemporaryDefinition) {
                        if (protocol == primaryProtocol) {
                            // Send the temporary to everyone relevant
                            (protocolAnalysis.protocols(it) - protocol).forEach { protocol ->
                                result.add(
                                    SendNode(
                                        ReadNode(it.temporary),
                                        ProtocolNode(protocol, it.temporary.sourceLocation),
                                        it.sourceLocation
                                    )
                                )
                            }
                        } else {
                            // Receive the temporary from the primary protocol
                            result.add(
                                ReceiveNode(
                                    it.temporary,
                                    ValueTypeNode(
                                        typeAnalysis.type(it),
                                        it.temporary.sourceLocation
                                    ),
                                    ProtocolNode(primaryProtocol, it.temporary.sourceLocation),
                                    it.sourceLocation
                                )
                            )
                        }
                    }
                    result
                }

                is IfNode ->
                    listOf(
                        IfNode(
                            it.guard,
                            it.thenBranch.projectFor(protocol),
                            it.elseBranch.projectFor(protocol),
                            it.sourceLocation
                        )
                    )

                is InfiniteLoopNode ->
                    listOf(
                        InfiniteLoopNode(
                            it.body.projectFor(protocol),
                            it.jumpLabel,
                            it.sourceLocation
                        )
                    )

                is BreakNode ->
                    listOf(it)

                is AssertionNode ->
                    listOf(it)

                is BlockNode ->
                    listOf(it.projectFor(protocol))
            }
        }

        return BlockNode(statements, this.sourceLocation)
    }

    return protocolAnalysis.protocols(body).associateWith { body.projectFor(it) }
}
