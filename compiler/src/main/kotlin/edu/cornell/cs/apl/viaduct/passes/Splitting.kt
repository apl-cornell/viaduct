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
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.deepCopy
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

class Splitter(
    private val protocolAnalysis: ProtocolAnalysis
) {
    companion object {
        private const val SYNC_NAME = "${'$'}sync"
    }

    private inner class ProgramSplitter(
        private val program: ProgramNode
    ) {
        private val nameGenerator = FreshNameGenerator()
        private val nameAnalysis = NameAnalysis.get(program)
        private val typeAnalysis = TypeAnalysis.get(program)
        private val functionMap: Map<Pair<FunctionName, Protocol>, FunctionName> =
            program.functions
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

        private fun ExpressionNode.eraseSecurityLabels(): ExpressionNode =
            when (this) {
                is LiteralNode -> this
                is ReadNode -> this
                is OperatorApplicationNode -> this
                is QueryNode -> this
                is DeclassificationNode -> this.expression
                is EndorsementNode -> this.expression
                is InputNode -> this
                is ReceiveNode -> this
            }

        private fun SimpleStatementNode.eraseSecurityLabels(): SimpleStatementNode =
            when (this) {
                is LetNode ->
                    LetNode(
                        temporary = this.temporary,
                        value = this.value.eraseSecurityLabels(),
                        sourceLocation = this.sourceLocation
                    )

                is DeclarationNode ->
                    DeclarationNode(
                        name = this.name,
                        className = this.className,
                        typeArguments = this.typeArguments,
                        labelArguments = null,
                        arguments = this.arguments,
                        sourceLocation = this.sourceLocation
                    )

                is UpdateNode -> this.deepCopy() as SimpleStatementNode

                is OutParameterInitializationNode ->
                    when (val initializer = this.initializer) {
                        is OutParameterExpressionInitializerNode ->
                            this.deepCopy() as SimpleStatementNode

                        is OutParameterConstructorInitializerNode ->
                            OutParameterInitializationNode(
                                name = this.name,
                                initializer =
                                    OutParameterConstructorInitializerNode(
                                        className = initializer.className,
                                        typeArguments = initializer.typeArguments,
                                        labelArguments = null,
                                        arguments = initializer.arguments,
                                        sourceLocation = initializer.sourceLocation
                                    ),
                                sourceLocation = this.sourceLocation
                            )
                    }

                is OutputNode -> this.deepCopy() as SimpleStatementNode

                is SendNode -> this.deepCopy() as SimpleStatementNode
            }

        /** Send synchronization to protocols that required synchronization in a control context
         *  they were not participating in. */
        private fun syncAfterControlStructure(
            statement: StatementNode,
            protocol: Protocol,
            newStatement: StatementNode
        ): List<StatementNode> =
            when (statement) {
                is IfNode, is InfiniteLoopNode, is BlockNode -> {
                    val protocols = protocolAnalysis.protocols(statement)
                    val protocolsToSync = protocolAnalysis.protocolsToSync(statement)
                    when (protocol) {
                        in protocols -> {
                            listOf(newStatement).plus(
                                protocolsToSync.map { protocolToSync ->
                                    SendNode(
                                        LiteralNode(UnitValue, statement.sourceLocation),
                                        ProtocolNode(protocolToSync, statement.sourceLocation),
                                        statement.sourceLocation
                                    )
                                }
                            )
                        }

                        in protocolsToSync -> {
                            protocols.map { protocolSyncFrom ->
                                LetNode(
                                    TemporaryNode(
                                        Temporary(nameGenerator.getFreshName(SYNC_NAME)),
                                        statement.sourceLocation
                                    ),
                                    ReceiveNode(
                                        ValueTypeNode(UnitType, statement.sourceLocation),
                                        ProtocolNode(protocolSyncFrom, statement.sourceLocation),
                                        statement.sourceLocation
                                    ),
                                    statement.sourceLocation
                                )
                            }
                        }

                        else -> listOf()
                    }
                }

                else -> throw Error("can only sync after if, loop, and block nodes")
            }

        private fun projectFor(block: BlockNode, protocol: Protocol): BlockNode {
            val statements: List<StatementNode> = block.statements.flatMap {
                if (protocol !in protocolAnalysis.participatingProtocols(it))
                    listOf()

                else when (it) {
                    is SimpleStatementNode -> {
                        val result = mutableListOf<StatementNode>()
                        val primaryProtocol = protocolAnalysis.primaryProtocol(it)

                        if (protocol == primaryProtocol)
                            result.add(it.eraseSecurityLabels())

                        if (it is LetNode) {
                            when (protocol) {
                                primaryProtocol -> {
                                    // Send the temporary to everyone relevant
                                    (protocolAnalysis.protocols(it) - primaryProtocol).forEach { sendProtocol ->
                                        result.add(
                                            SendNode(
                                                ReadNode(it.temporary),
                                                ProtocolNode(sendProtocol, it.temporary.sourceLocation),
                                                it.sourceLocation
                                            )
                                        )
                                    }
                                }

                                // Receive the temporary from the primary protocol
                                in protocolAnalysis.protocols(it) -> {
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
                        }

                        // handle synchronization
                        val protocolsToSync = protocolAnalysis.protocolsToSync(it)

                        if (it is LetNode) {
                            when (protocol) {
                                // send synchronization
                                primaryProtocol -> {
                                    protocolsToSync.forEach { syncProtocol ->
                                        result.add(
                                            SendNode(
                                                LiteralNode(UnitValue, it.sourceLocation),
                                                ProtocolNode(syncProtocol, it.sourceLocation),
                                                it.sourceLocation
                                            )
                                        )
                                    }
                                }

                                // receive synchronization from primary protocol
                                in protocolsToSync -> {
                                    result.add(
                                        LetNode(
                                            TemporaryNode(
                                                Temporary(nameGenerator.getFreshName(SYNC_NAME)),
                                                it.sourceLocation
                                            ),
                                            ReceiveNode(
                                                ValueTypeNode(UnitType, it.sourceLocation),
                                                ProtocolNode(primaryProtocol, it.sourceLocation),
                                                it.sourceLocation
                                            ),
                                            it.sourceLocation
                                        )
                                    )
                                }
                            }
                        }

                        result
                    }

                    is FunctionCallNode -> {
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
                    }

                    is IfNode ->
                        syncAfterControlStructure(
                            it,
                            protocol,
                            IfNode(
                                it.guard.deepCopy() as AtomicExpressionNode,
                                projectFor(it.thenBranch, protocol),
                                projectFor(it.elseBranch, protocol),
                                it.sourceLocation
                            )
                        )

                    is InfiniteLoopNode ->
                        syncAfterControlStructure(
                            it,
                            protocol,
                            InfiniteLoopNode(
                                projectFor(it.body, protocol),
                                it.jumpLabel,
                                it.sourceLocation
                            )
                        )

                    is BreakNode ->
                        listOf(it.deepCopy() as StatementNode)

                    is AssertionNode ->
                        listOf(it.deepCopy() as StatementNode)

                    is BlockNode ->
                        syncAfterControlStructure(
                            it,
                            protocol,
                            projectFor(it, protocol)
                        )
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
        private fun split(process: ProcessDeclarationNode): List<ProcessDeclarationNode> {
            return protocolAnalysis.protocols(process.body).map {
                ProcessDeclarationNode(
                    protocol = ProtocolNode(it, process.protocol.sourceLocation),
                    body = projectFor(process.body, it),
                    sourceLocation = process.sourceLocation
                )
            }
        }

        private fun split(function: FunctionDeclarationNode): List<FunctionDeclarationNode> {
            return protocolAnalysis.protocols(function)
                .map { protocol ->
                    FunctionDeclarationNode(
                        Located(
                            functionMap[Pair(function.name.value, protocol)]!!,
                            function.name.sourceLocation
                        ),
                        null,
                        Arguments(
                            function.parameters.filter { param ->
                                protocolAnalysis.primaryProtocol(param) == protocol
                            },
                            function.parameters.sourceLocation
                        ),
                        projectFor(function.body, protocol),
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
        fun splitMain(): ProgramNode {
            // Assert that the program has a main
            program.main

            val reachableFunctions = nameAnalysis.reachableFunctions(program.main)
            val splitDeclarations: MutableList<TopLevelDeclarationNode> = mutableListOf()
            for (declaration in program.declarations) {
                when {
                    declaration is ProcessDeclarationNode
                        && declaration.protocol.value == MainProtocol ->
                    {
                        splitDeclarations.addAll(split(declaration))
                    }

                    declaration is FunctionDeclarationNode
                        && reachableFunctions.contains(declaration.name.value) ->
                    {
                        splitDeclarations.addAll(split(declaration))
                    }

                    else -> {
                        splitDeclarations.add(declaration)
                    }
                }
            }

            return ProgramNode(splitDeclarations, program.sourceLocation)
        }
    }

    fun splitMain(): ProgramNode =
        ProgramSplitter(protocolAnalysis.program).splitMain()
}
