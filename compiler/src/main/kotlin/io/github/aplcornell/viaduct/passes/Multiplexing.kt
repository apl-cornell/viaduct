package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.freshVariableNameGenerator
import io.github.aplcornell.viaduct.errors.UnknownDatatypeError
import io.github.aplcornell.viaduct.errors.UnknownMethodError
import io.github.aplcornell.viaduct.selection.ProtocolAssignment
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Operator
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.TemporaryNode
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.Modify
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Set
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.AssertionNode
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.BreakNode
import io.github.aplcornell.viaduct.syntax.intermediate.CommunicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DelegationDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutputNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.TopLevelDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.intermediate.deepCopy
import io.github.aplcornell.viaduct.syntax.operators.And
import io.github.aplcornell.viaduct.syntax.operators.Mux
import io.github.aplcornell.viaduct.syntax.operators.Not
import io.github.aplcornell.viaduct.util.FreshNameGenerator

fun StatementNode.canMux(): Boolean =
    when (this) {
        is LetNode ->
            when (this.value) {
                is CommunicationNode -> false
                else -> true
            }

        is DeclarationNode -> true

        is UpdateNode -> true

        is OutParameterInitializationNode -> false

        is OutputNode -> false

        is FunctionCallNode -> false

        is IfNode -> this.thenBranch.canMux() && this.elseBranch.canMux()

        is InfiniteLoopNode -> false

        is BreakNode -> false

        is AssertionNode -> false

        is BlockNode -> this.statements.all { it.canMux() }
    }

class MuxPostprocessor(
    val containedProtocolCheck: (Protocol) -> Boolean,
    val selection: ProtocolAssignment,
) : ProgramPostprocessor {
    override fun postprocess(program: ProgramNode): ProgramNode {
        val nameAnalysis = program.analyses.get<NameAnalysis>()
        return ProgramNode(
            program.declarations.map { declaration ->
                when (declaration) {
                    is HostDeclarationNode -> declaration.deepCopy() as TopLevelDeclarationNode

                    is DelegationDeclarationNode -> declaration.deepCopy() as TopLevelDeclarationNode

                    is FunctionDeclarationNode -> {
                        FunctionDeclarationNode(
                            declaration.name,
                            declaration.labelParameters,
                            Arguments(
                                declaration.parameters.map { it.deepCopy() as ParameterNode },
                                declaration.parameters.sourceLocation,
                            ),
                            declaration.labelConstraints,
                            declaration.pcLabel,
                            mux(
                                declaration.body,
                                nameAnalysis,
                                declaration.freshVariableNameGenerator(),
                            ),
                            declaration.sourceLocation,
                        )
                    }
                }
            },
            program.sourceLocation,
        )
    }

    fun mux(
        block: BlockNode,
        nameAnalysis: NameAnalysis,
        nameGenerator: FreshNameGenerator = block.freshVariableNameGenerator(),
    ): BlockNode {
        val newStatements = mutableListOf<StatementNode>()
        for (child in block.statements) {
            newStatements.addAll(
                asStraightLine(child, nameAnalysis, nameGenerator, null),
            )
        }

        return BlockNode(newStatements, block.sourceLocation)
    }

    private fun asStraightLine(
        stmt: StatementNode,
        nameAnalysis: NameAnalysis,
        nameGenerator: FreshNameGenerator,
        currentGuard: TemporaryNode?,
    ): List<StatementNode> =
        when (stmt) {
            is LetNode -> listOf(stmt.deepCopy() as StatementNode)

            is DeclarationNode -> listOf(stmt.deepCopy() as StatementNode)

            is UpdateNode -> {
                if (currentGuard != null) {
                    val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                    val updateProtocol = selection.getAssignment(enclosingFunction, stmt.variable.value)
                    val objectType = nameAnalysis.objectType(nameAnalysis.declaration(stmt))
                    val className = objectType.className.value
                    val getTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$GET_TEMPORARY_NAME")),
                            stmt.sourceLocation,
                        )

                    val muxTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$MUX_TEMPORARY_NAME")),
                            stmt.sourceLocation,
                        )

                    val indexExpr: () -> AtomicExpressionNode? = {
                        when (className) {
                            MutableCell -> null
                            Vector -> stmt.arguments[0].deepCopy() as AtomicExpressionNode
                            else -> throw UnknownDatatypeError(stmt.variable, className)
                        }
                    }

                    val getCall =
                        LetNode(
                            getTemporary,
                            QueryNode(
                                stmt.variable,
                                Located(Get, stmt.sourceLocation),
                                Arguments(
                                    indexExpr()?.let { listOf(it) } ?: listOf(),
                                    stmt.sourceLocation,
                                ),
                                stmt.sourceLocation,
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            stmt.sourceLocation,
                        )

                    val muxCall = { arg: AtomicExpressionNode ->
                        LetNode(
                            muxTemporary,
                            OperatorApplicationNode(
                                Mux,
                                Arguments(
                                    listOf(ReadNode(currentGuard), arg, ReadNode(getTemporary)),
                                    stmt.sourceLocation,
                                ),
                                stmt.sourceLocation,
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            stmt.sourceLocation,
                        )
                    }

                    val operationTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$OP_TEMPORARY_NAME")),
                            stmt.arguments.sourceLocation,
                        )

                    val operatorCall = { operator: Operator, arg: AtomicExpressionNode ->
                        LetNode(
                            operationTemporary,
                            OperatorApplicationNode(
                                operator,
                                Arguments(
                                    listOf(ReadNode(getTemporary), arg),
                                    arg.sourceLocation,
                                ),
                                arg.sourceLocation,
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            arg.sourceLocation,
                        )
                    }

                    val updateCall =
                        UpdateNode(
                            stmt.variable,
                            Located(
                                Set,
                                stmt.update.sourceLocation,
                            ),
                            Arguments(
                                indexExpr()?.let {
                                    listOf(it, ReadNode(muxTemporary))
                                } ?: listOf(ReadNode(muxTemporary)),
                                stmt.sourceLocation,
                            ),
                            stmt.sourceLocation,
                        )

                    when (val update = stmt.update.value) {
                        is Set -> {
                            listOf(
                                getCall,
                                muxCall(indexExpr() ?: stmt.arguments[0].deepCopy() as AtomicExpressionNode),
                                updateCall,
                            )
                        }

                        is Modify -> {
                            listOf(
                                getCall,
                                operatorCall(
                                    update.operator,
                                    indexExpr() ?: stmt.arguments[0].deepCopy() as AtomicExpressionNode,
                                ),
                                muxCall(ReadNode(operationTemporary)),
                                updateCall,
                            )
                        }

                        else -> throw UnknownMethodError(stmt.variable, stmt.update)
                    }
                } else {
                    listOf(stmt.deepCopy() as StatementNode)
                }
            }

            is OutParameterInitializationNode -> listOf(stmt.deepCopy() as StatementNode)

            is OutputNode -> listOf(stmt.deepCopy() as StatementNode)

            is FunctionCallNode -> listOf(stmt.deepCopy() as StatementNode)

            is IfNode -> {
                val guardProtocol: Protocol? =
                    when (stmt.guard) {
                        is ReadNode -> {
                            val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                            selection.getAssignment(enclosingFunction, stmt.guard.temporary.value)
                        }

                        is LiteralNode -> null
                    }

                when {
                    guardProtocol != null && containedProtocolCheck(guardProtocol) && stmt.canMux() -> {
                        val negatedGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation,
                            )
                        val pathGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation,
                            )
                        val negatedPathGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation,
                            )

                        val newStatements = mutableListOf<StatementNode>()

                        newStatements.add(
                            LetNode(
                                negatedGuard,
                                OperatorApplicationNode(
                                    Not,
                                    Arguments(
                                        listOf(stmt.guard.deepCopy() as AtomicExpressionNode),
                                        stmt.guard.sourceLocation,
                                    ),
                                    stmt.guard.sourceLocation,
                                ),
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation,
                            ),
                        )

                        newStatements.add(
                            LetNode(
                                pathGuard,
                                currentGuard?.let {
                                    OperatorApplicationNode(
                                        And,
                                        Arguments(
                                            listOf(
                                                stmt.guard.deepCopy() as AtomicExpressionNode,
                                                ReadNode(it),
                                            ),
                                            stmt.guard.sourceLocation,
                                        ),
                                        stmt.guard.sourceLocation,
                                    )
                                } ?: stmt.guard.deepCopy() as AtomicExpressionNode,
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation,
                            ),
                        )

                        newStatements.add(
                            LetNode(
                                negatedPathGuard,
                                currentGuard?.let {
                                    OperatorApplicationNode(
                                        And,
                                        Arguments(
                                            listOf(ReadNode(negatedGuard), ReadNode(it)),
                                            stmt.guard.sourceLocation,
                                        ),
                                        stmt.guard.sourceLocation,
                                    )
                                } ?: ReadNode(negatedGuard),
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation,
                            ),
                        )

                        newStatements.addAll(
                            asStraightLine(
                                stmt.thenBranch,
                                nameAnalysis,
                                nameGenerator,
                                pathGuard,
                            ),
                        )
                        newStatements.addAll(
                            asStraightLine(
                                stmt.elseBranch,
                                nameAnalysis,
                                nameGenerator,
                                negatedPathGuard,
                            ),
                        )

                        newStatements
                    }

                    else -> {
                        assert(currentGuard == null)
                        listOf(
                            IfNode(
                                stmt.guard.deepCopy() as AtomicExpressionNode,
                                mux(stmt.thenBranch, nameAnalysis, nameGenerator),
                                mux(stmt.elseBranch, nameAnalysis, nameGenerator),
                                stmt.sourceLocation,
                            ),
                        )
                    }
                }
            }

            is InfiniteLoopNode -> {
                assert(currentGuard == null)
                listOf(
                    InfiniteLoopNode(
                        mux(stmt.body, nameAnalysis, nameGenerator),
                        stmt.jumpLabel,
                        stmt.sourceLocation,
                    ),
                )
            }

            is BreakNode -> listOf(stmt.deepCopy() as StatementNode)

            is AssertionNode -> listOf(stmt.deepCopy() as StatementNode)

            is BlockNode -> stmt.statements.flatMap { child ->
                asStraightLine(child, nameAnalysis, nameGenerator, currentGuard)
            }
        }

    companion object {
        private const val GET_TEMPORARY_NAME = "get"
        private const val MUX_TEMPORARY_NAME = "mux"
        private const val OP_TEMPORARY_NAME = "op"
        private const val GUARD_TEMPORARY_NAME = "guard"
    }
}
