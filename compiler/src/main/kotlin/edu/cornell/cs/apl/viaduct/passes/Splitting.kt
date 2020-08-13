package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.protocols.Ideal
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

class Splitter(
    val nameAnalysis: NameAnalysis,
    val protocolAnalysis: ProtocolAnalysis,
    val typeAnalysis: TypeAnalysis
) {
    private fun projectFor(
        functionMap: Map<Pair<FunctionName, Protocol>, FunctionName>,
        block: BlockNode,
        protocol: Protocol
    ): BlockNode {
        val statements: List<StatementNode> = block.statements.flatMap {
            if (protocol !in protocolAnalysis.protocols(it))
                listOf()
            else when (it) {
                is SimpleStatementNode -> {
                    val result = mutableListOf<StatementNode>()
                    val primaryProtocol = protocolAnalysis.primaryProtocol(it)

                    if (protocol == primaryProtocol)
                        result.add(it)

                    if (it is LetNode) {
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
                                LetNode(
                                    it.temporary,
                                    ReceiveNode(
                                        ValueTypeNode(
                                            typeAnalysis.type(it),
                                            it.value.sourceLocation
                                        ),
                                        ProtocolNode(primaryProtocol, it.temporary.sourceLocation),
                                        it.value.sourceLocation
                                    ),
                                    it.sourceLocation
                                )
                            )
                        }
                    }
                    result
                }

                is FunctionCallNode ->
                    listOf(
                        FunctionCallNode(
                            Located(
                                functionMap[Pair(it.name.value, protocol)]!!,
                                it.name.sourceLocation
                            ),
                            Arguments(
                                it.arguments.filter { arg ->
                                    protocolAnalysis.primaryProtocol(nameAnalysis.parameter(arg)) == protocol
                                },
                                it.arguments.sourceLocation
                            ),
                            it.sourceLocation
                        )
                    )

                is IfNode ->
                    listOf(
                        IfNode(
                            it.guard.deepCopy() as AtomicExpressionNode,
                            projectFor(functionMap, it.thenBranch, protocol),
                            projectFor(functionMap, it.elseBranch, protocol),
                            it.sourceLocation
                        )
                    )

                is InfiniteLoopNode ->
                    listOf(
                        InfiniteLoopNode(
                            projectFor(functionMap, it.body, protocol),
                            it.jumpLabel,
                            it.sourceLocation
                        )
                    )

                is BreakNode ->
                    listOf(it.deepCopy() as StatementNode)

                is AssertionNode ->
                    listOf(it.deepCopy() as StatementNode)

                is BlockNode ->
                    listOf(projectFor(functionMap, it, protocol))
            }
        }

        return BlockNode(statements, block.sourceLocation)
    }

    /**
     * Returns a list of [ProcessDeclarationNode]s that together implement [this] [Ideal] functionality.
     * The result contains an entry for each [Protocol] involved in the execution of [this] process.
     * The code assigned to each [Protocol] is the body of [this] process with parts irrelevant to
     * that [Protocol] erased.
     */
    private fun split(
        functionMap: Map<Pair<FunctionName, Protocol>, FunctionName>,
        process: ProcessDeclarationNode
    ): List<ProcessDeclarationNode> {
        return protocolAnalysis.protocols(process.body).map {
            ProcessDeclarationNode(
                protocol = ProtocolNode(it, process.protocol.sourceLocation),
                body = projectFor(functionMap, process.body, it),
                sourceLocation = process.sourceLocation
            )
        }
    }

    private fun split(
        functionMap: Map<Pair<FunctionName, Protocol>, FunctionName>,
        function: FunctionDeclarationNode
    ): List<FunctionDeclarationNode> {
        return protocolAnalysis.protocols(function)
            .map { protocol ->
                FunctionDeclarationNode(
                    Located(
                        functionMap[Pair(function.name.value, protocol)]!!,
                        function.name.sourceLocation
                    ),
                    function.pcLabel,
                    Arguments(
                        function.parameters.filter { param ->
                            protocolAnalysis.primaryProtocol(param) == protocol
                        },
                        function.parameters.sourceLocation
                    ),
                    projectFor(functionMap, function.body, protocol),
                    function.sourceLocation
                )
            }
    }

    /**
     * Splits the [MainProtocol] in this program using [ProcessDeclarationNode.split], preserving all
     * other [TopLevelDeclarationNode]s.
     *
     * @throws NoMainError if [this] program does not have a [MainProtocol].
     */
    // TODO: rewrite all references to main in other protocols
    // TODO: maybe generalize from main to an arbitrary process?
    fun splitMain(program: ProgramNode): ProgramNode {
        // Assert that the program has a main
        program.main

        val nameGenerator = FreshNameGenerator()
        val declarations: MutableList<TopLevelDeclarationNode> = mutableListOf()

        // split functions first so we can rewrite call sites
        // compute new function names for each projection
        val functionMap: Map<Pair<FunctionName, Protocol>, FunctionName> =
            program.declarations.filterIsInstance<FunctionDeclarationNode>()
                .flatMap { function ->
                    protocolAnalysis
                        .protocols(function)
                        .map { protocol ->
                            val name = "${function.name.value.name}_${protocol.protocolName.name}"
                            Pair(
                                Pair(function.name.value, protocol),
                                FunctionName(nameGenerator.getFreshName(name))
                            )
                        }
                }
                .toMap()

        for (declaration in program.declarations) {
            when {
                declaration is ProcessDeclarationNode && declaration.protocol.value == MainProtocol -> {
                    declarations.addAll(split(functionMap, declaration))
                }

                declaration is FunctionDeclarationNode -> {
                    declarations.addAll(split(functionMap, declaration))
                }

                else -> {
                    declarations.add(declaration)
                }
            }
        }

        return ProgramNode(declarations, program.sourceLocation)
    }

    /** Like [Node.copy], but recursively copies all descendant nodes also.*/
    private fun Node.deepCopy(): Node =
        this.copy(this.children.toList().map { it.deepCopy() })
}
