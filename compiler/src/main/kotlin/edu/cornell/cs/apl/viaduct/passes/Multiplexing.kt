package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.freshVariableNameGenerator
import edu.cornell.cs.apl.viaduct.errors.UnknownDatatypeError
import edu.cornell.cs.apl.viaduct.errors.UnknownMethodError
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.deepCopy
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

fun StatementNode.canMux(): Boolean =
    when (this) {
        is LetNode ->
            when (this.value) {
                is InputNode -> false
                is ReceiveNode -> false
                else -> true
            }

        is DeclarationNode -> true

        is UpdateNode -> true

        is OutParameterInitializationNode -> false

        is OutputNode -> false

        is SendNode -> false

        is FunctionCallNode -> false

        is IfNode -> this.thenBranch.canMux() && this.elseBranch.canMux()

        is InfiniteLoopNode -> false

        is BreakNode -> false

        is AssertionNode -> false

        is BlockNode -> this.statements.all { it.canMux() }
    }

class MuxPostprocessor(
    val containedProtocolCheck: (Protocol) -> Boolean,
    val protocolAssignment: (FunctionName, Variable) -> Protocol
) : ProgramPostprocessor {
    override fun postprocess(program: ProgramNode): ProgramNode {
        val nameAnalysis = NameAnalysis.get(program)
        return ProgramNode(
            program.declarations.map { declaration ->
                when (declaration) {
                    is HostDeclarationNode -> declaration.deepCopy() as TopLevelDeclarationNode

                    is ProcessDeclarationNode -> {
                        if (declaration.protocol.value == MainProtocol) {
                            ProcessDeclarationNode(
                                declaration.protocol,
                                mux(declaration.body, nameAnalysis),
                                declaration.sourceLocation
                            )
                        } else {
                            declaration.deepCopy() as ProcessDeclarationNode
                        }
                    }

                    is FunctionDeclarationNode -> {
                        FunctionDeclarationNode(
                            declaration.name,
                            declaration.pcLabel,
                            Arguments(
                                declaration.parameters.map { it.deepCopy() as ParameterNode },
                                declaration.parameters.sourceLocation
                            ),
                            mux(
                                declaration.body,
                                nameAnalysis,
                                declaration.freshVariableNameGenerator()
                            ),
                            declaration.sourceLocation
                        )
                    }
                }
            },
            program.sourceLocation
        )
    }

    fun mux(
        block: BlockNode,
        nameAnalysis: NameAnalysis,
        nameGenerator: FreshNameGenerator = block.freshVariableNameGenerator()
    ): BlockNode {
        val newStatements = mutableListOf<StatementNode>()
        for (child in block.statements) {
            newStatements.addAll(
                asStraightLine(child, nameAnalysis, nameGenerator, null)
            )
        }

        return BlockNode(newStatements, block.sourceLocation)
    }

    private fun asStraightLine(
        stmt: StatementNode,
        nameAnalysis: NameAnalysis,
        nameGenerator: FreshNameGenerator,
        currentGuard: TemporaryNode?
    ): List<StatementNode> =
        when (stmt) {
            is LetNode -> listOf(stmt.deepCopy() as StatementNode)

            is DeclarationNode -> listOf(stmt.deepCopy() as StatementNode)

            is UpdateNode -> {
                if (currentGuard != null) {
                    val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                    val updateProtocol = protocolAssignment(enclosingFunction, stmt.variable.value)
                    val className = nameAnalysis.declaration(stmt).className.value
                    val getTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$GET_TEMPORARY_NAME")),
                            stmt.sourceLocation
                        )

                    val muxTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$MUX_TEMPORARY_NAME")),
                            stmt.sourceLocation
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
                                    stmt.sourceLocation
                                ),
                                stmt.sourceLocation
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            stmt.sourceLocation
                        )

                    val muxCall = { arg: AtomicExpressionNode ->
                        LetNode(
                            muxTemporary,
                            OperatorApplicationNode(
                                Mux,
                                Arguments(
                                    listOf(ReadNode(currentGuard), arg, ReadNode(getTemporary)),
                                    stmt.sourceLocation
                                ),
                                stmt.sourceLocation
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            stmt.sourceLocation
                        )
                    }

                    val operationTemporary =
                        Located(
                            Temporary(nameGenerator.getFreshName("${'$'}$OP_TEMPORARY_NAME")),
                            stmt.arguments.sourceLocation
                        )

                    val operatorCall = { operator: Operator, arg: AtomicExpressionNode ->
                        LetNode(
                            operationTemporary,
                            OperatorApplicationNode(
                                operator,
                                Arguments(
                                    listOf(ReadNode(getTemporary), arg),
                                    arg.sourceLocation
                                ),
                                arg.sourceLocation
                            ),
                            Located(updateProtocol, stmt.sourceLocation),
                            arg.sourceLocation
                        )
                    }

                    val updateCall =
                        UpdateNode(
                            stmt.variable,
                            Located(
                                Set,
                                stmt.update.sourceLocation
                            ),
                            Arguments(
                                indexExpr()?.let {
                                    listOf(it, ReadNode(muxTemporary))
                                } ?: listOf(ReadNode(muxTemporary)),
                                stmt.sourceLocation
                            ),
                            stmt.sourceLocation
                        )

                    when (val update = stmt.update.value) {
                        is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                            listOf(
                                getCall,
                                muxCall(indexExpr() ?: stmt.arguments[0].deepCopy() as AtomicExpressionNode),
                                updateCall
                            )
                        }

                        is Modify -> {
                            listOf(
                                getCall,
                                operatorCall(
                                    update.operator,
                                    indexExpr() ?: stmt.arguments[0].deepCopy() as AtomicExpressionNode
                                ),
                                muxCall(ReadNode(operationTemporary)),
                                updateCall
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

            is SendNode -> listOf(stmt.deepCopy() as StatementNode)

            is FunctionCallNode -> listOf(stmt.deepCopy() as StatementNode)

            is IfNode -> {
                val guardProtocol: Protocol? =
                    when (stmt.guard) {
                        is ReadNode -> {
                            val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                            protocolAssignment(enclosingFunction, stmt.guard.temporary.value)
                        }

                        is LiteralNode -> null
                    }

                when {
                    guardProtocol != null && containedProtocolCheck(guardProtocol) && stmt.canMux() -> {
                        val negatedGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation
                            )
                        val pathGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation
                            )
                        val negatedPathGuard =
                            Located(
                                Temporary(nameGenerator.getFreshName("${'$'}$GUARD_TEMPORARY_NAME")),
                                stmt.guard.sourceLocation
                            )

                        val newStatements = mutableListOf<StatementNode>()

                        newStatements.add(
                            LetNode(
                                negatedGuard,
                                OperatorApplicationNode(
                                    Not,
                                    Arguments(
                                        listOf(stmt.guard.deepCopy() as AtomicExpressionNode),
                                        stmt.guard.sourceLocation
                                    ),
                                    stmt.guard.sourceLocation
                                ),
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation
                            )
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
                                                ReadNode(it)
                                            ),
                                            stmt.guard.sourceLocation
                                        ),
                                        stmt.guard.sourceLocation
                                    )
                                } ?: stmt.guard.deepCopy() as AtomicExpressionNode,
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation
                            )
                        )

                        newStatements.add(
                            LetNode(
                                negatedPathGuard,
                                currentGuard?.let {
                                    OperatorApplicationNode(
                                        And,
                                        Arguments(
                                            listOf(ReadNode(negatedGuard), ReadNode(it)),
                                            stmt.guard.sourceLocation
                                        ),
                                        stmt.guard.sourceLocation
                                    )
                                } ?: ReadNode(negatedGuard),
                                Located(guardProtocol, stmt.guard.sourceLocation),
                                stmt.guard.sourceLocation
                            )
                        )

                        newStatements.addAll(
                            asStraightLine(
                                stmt.thenBranch,
                                nameAnalysis,
                                nameGenerator,
                                pathGuard
                            )
                        )
                        newStatements.addAll(
                            asStraightLine(
                                stmt.elseBranch,
                                nameAnalysis,
                                nameGenerator,
                                negatedPathGuard
                            )
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
                                stmt.sourceLocation
                            )
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
                        stmt.sourceLocation
                    )
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
