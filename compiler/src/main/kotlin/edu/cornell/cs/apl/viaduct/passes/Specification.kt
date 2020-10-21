package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Adversary
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.protocols.Ideal
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode

/**
 * Returns a universal composability (UC) ideal functionality that captures the security behavior of [this] program.
 *
 * Essentially, this function instruments each process so that any data that the adversary is allowed to see is
 * explicitly sent to [Adversary], and any data that the adversary is allowed to corrupt is received from [Adversary].
 *
 * Data leakage and corruption is done at [LetNode]s. What data the adversary can see or influence is determined by
 * comparing [adversaryLabel] to the [Label]s of the [Temporary] variables defined by the [LetNode]s. Labels of the
 * temporaries are computed based on the protocol of the process they are in. For an [Ideal] protocol, the label is
 * determined using information flow analysis. For any other protocol, the label is the same as [Protocol.authority].
 * Finally, processes where the protocol is a [HostInterface] are simply removed, since these processes are debug only.
 */
fun ProgramNode.specification(adversaryLabel: Label): ProgramNode {
    val typeAnalysis = TypeAnalysis.get(this)
    val informationFlowAnalysis = InformationFlowAnalysis.get(this)
    val hostTrustConfiguration = HostTrustConfiguration(this)

    val declarations =
        this.declarations.filterNot { it is ProcessDeclarationNode && it.protocol.value is HostInterface }.map {
            if (it is ProcessDeclarationNode) {
                when (it.protocol.value) {
                    is Ideal ->
                        it.specification(adversaryLabel, informationFlowAnalysis::label, typeAnalysis)
                    else ->
                        it.specification(
                            adversaryLabel,
                            { _ -> it.protocol.value.authority(hostTrustConfiguration) },
                            typeAnalysis
                        )
                }
            } else {
                it
            }
        }

    return ProgramNode(declarations, sourceLocation)
}

private fun ProcessDeclarationNode.specification(
    adversaryLabel: Label,
    labelAssignment: (LetNode) -> Label,
    typeAnalysis: TypeAnalysis
): ProcessDeclarationNode {
    fun Label.integrityIsCompromised() = adversaryLabel.integrity().actsFor(this.integrity())

    fun Label.confidentialityIsCompromised() = adversaryLabel.confidentiality().actsFor(this.confidentiality())

    fun BlockNode.specification(): BlockNode {
        val statements: List<StatementNode> = statements.flatMap {
            when (it) {
                is LetNode -> {
                    val label = labelAssignment(it)
                    val result: MutableList<StatementNode> = mutableListOf()

                    // Compute the actual value
                    result.add(it)

                    // Leak the value to the adversary
                    if (label.confidentialityIsCompromised()) {
                        result.add(
                            SendNode(
                                ReadNode(it.temporary),
                                ProtocolNode(Adversary, it.temporary.sourceLocation),
                                it.temporary.sourceLocation
                            )
                        )
                    }

                    // Let the adversary change the value
                    if (label.integrityIsCompromised()) {
                        val maul =
                            LetNode(
                                it.temporary,
                                ReceiveNode(
                                    ValueTypeNode(typeAnalysis.type(it), it.temporary.sourceLocation),
                                    ProtocolNode(Adversary, it.value.sourceLocation),
                                    it.value.sourceLocation
                                ),
                                null,
                                it.sourceLocation
                            )

                        // We need to put the original let statement in a block to avoid name clashes
                        listOf(BlockNode(result, it.sourceLocation), maul)
                    } else {
                        result
                    }
                }

                is SimpleStatementNode ->
                    listOf(it)

                // TODO: is this right?
                is FunctionCallNode ->
                    listOf(it)

                is IfNode ->
                    listOf(
                        IfNode(
                            it.guard,
                            it.thenBranch.specification(),
                            it.elseBranch.specification(),
                            it.sourceLocation
                        )
                    )
                is InfiniteLoopNode ->
                    listOf(InfiniteLoopNode(it.body.specification(), it.jumpLabel, it.sourceLocation))
                is BreakNode ->
                    listOf(it)
                is AssertionNode ->
                    listOf(it)

                is BlockNode ->
                    listOf(it.specification())
            }
        }
        return BlockNode(statements, sourceLocation)
    }

    return ProcessDeclarationNode(protocol, body.specification(), sourceLocation)
}
