package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
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

fun ProgramNode.mux(
    muxedProcesses: Set<Protocol>,
    muxedFunctions: Set<FunctionName>
): ProgramNode {
    val nameAnalysis = NameAnalysis.get(this)
    return ProgramNode(
        this.declarations.map { declaration ->
            when (declaration) {
                is HostDeclarationNode -> declaration.deepCopy() as TopLevelDeclarationNode

                is ProcessDeclarationNode -> {
                    if (muxedProcesses.contains(declaration.protocol.value)) {
                        ProcessDeclarationNode(
                            declaration.protocol,
                            declaration.body.mux(nameAnalysis),
                            declaration.sourceLocation
                        )
                    } else {
                        declaration.deepCopy() as ProcessDeclarationNode
                    }
                }

                is FunctionDeclarationNode -> {
                    if (muxedFunctions.contains(declaration.name.value)) {
                        FunctionDeclarationNode(
                            declaration.name,
                            declaration.pcLabel,
                            Arguments(
                                declaration.parameters.map { it.deepCopy() as ParameterNode },
                                declaration.parameters.sourceLocation
                            ),
                            declaration.body.mux(nameAnalysis),
                            declaration.sourceLocation
                        )
                    } else {
                        declaration.deepCopy() as FunctionDeclarationNode
                    }
                }
            }
        },
        this.sourceLocation
    )
}

fun ProgramNode.mux(): ProgramNode =
    this.mux(
        this.declarations
            .filterIsInstance<ProcessDeclarationNode>()
            .map { procDecl -> procDecl.protocol.value }
            .toSet(),
        this.functions.map { f -> f.name.value }.toSet()
    )

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

        is BlockNode -> this.statements.all { child -> child.canMux() }
    }

fun BlockNode.mux(
    nameAnalysis: NameAnalysis,
    nameGenerator: FreshNameGenerator = FreshNameGenerator(nameAnalysis.declaredNames(this))
): BlockNode {
    val newStatements = mutableListOf<StatementNode>()
    for (child in this.statements) {
        if (child.canMux()) {
            newStatements.addAll(
                child.asStraightLine(nameAnalysis, nameGenerator, null)
            )
        } else {
            newStatements.add(child.deepCopy() as StatementNode)
        }
    }

    return BlockNode(newStatements, this.sourceLocation)
}

private fun StatementNode.asStraightLine(
    nameAnalysis: NameAnalysis,
    nameGenerator: FreshNameGenerator,
    currentGuard: TemporaryNode?
): List<StatementNode> =
    when (this) {
        is LetNode -> listOf(this.deepCopy() as StatementNode)

        is DeclarationNode -> listOf(this.deepCopy() as StatementNode)

        is UpdateNode -> {
            if (currentGuard != null) {
                val className = nameAnalysis.declaration(this).className.value
                val getTemporary =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}get")),
                        this.sourceLocation
                    )

                val muxTemporary =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}mux")),
                        this.sourceLocation
                    )

                val indexExpr: () -> AtomicExpressionNode? = {
                    when (className) {
                        MutableCell -> null
                        Vector -> this.arguments[0].deepCopy() as AtomicExpressionNode
                        else -> throw Error("unknown datatype")
                    }
                }

                val getCall =
                    LetNode(
                        getTemporary,
                        QueryNode(
                            this.variable,
                            Located(Get, this.sourceLocation),
                            Arguments(
                                indexExpr()?.let { listOf(it) } ?: listOf(),
                                this.sourceLocation
                            ),
                            this.sourceLocation
                        ),
                        this.sourceLocation
                    )

                val muxCall = { arg: AtomicExpressionNode ->
                    LetNode(
                        muxTemporary,
                        OperatorApplicationNode(
                            Mux,
                            Arguments(
                                listOf(ReadNode(currentGuard), arg, ReadNode(getTemporary)),
                                this.sourceLocation
                            ),
                            this.sourceLocation
                        ),
                        this.sourceLocation
                    )
                }

                val operationTemporary =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}operation")),
                        this.arguments.sourceLocation
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
                        arg.sourceLocation
                    )
                }

                val updateCall =
                    UpdateNode(
                        this.variable,
                        Located(
                            edu.cornell.cs.apl.viaduct.syntax.datatypes.Set,
                            this.update.sourceLocation
                        ),
                        Arguments(
                            indexExpr()?.let {
                                listOf(it, ReadNode(muxTemporary))
                            } ?: listOf(ReadNode(muxTemporary)),
                            this.sourceLocation
                        ),
                        this.sourceLocation
                    )

                when (val update = this.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                        listOf(
                            getCall,
                            muxCall(indexExpr() ?: this.arguments[0].deepCopy() as AtomicExpressionNode),
                            updateCall
                        )
                    }

                    is Modify -> {
                        listOf(
                            getCall,
                            operatorCall(
                                update.operator,
                                indexExpr() ?: this.arguments[0].deepCopy() as AtomicExpressionNode
                            ),
                            muxCall(ReadNode(operationTemporary)),
                            updateCall
                        )
                    }

                    else -> throw Error("unknown update name")
                }
            } else {
                listOf(this.deepCopy() as StatementNode)
            }
        }

        is OutParameterInitializationNode -> throw Error("mux error")

        is OutputNode -> throw Error("mux error")

        is SendNode -> throw Error("mux error")

        is FunctionCallNode -> throw Error("mux error")

        is IfNode -> {
            if (this.canMux()) {
                val negatedGuard =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}guard")),
                        this.guard.sourceLocation
                    )
                val pathGuard =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}guard")),
                        this.guard.sourceLocation
                    )
                val negatedPathGuard =
                    Located(
                        Temporary(nameGenerator.getFreshName("${'$'}guard")),
                        this.guard.sourceLocation
                    )

                val newStatements = mutableListOf<StatementNode>()

                newStatements.add(
                    LetNode(
                        negatedGuard,
                        OperatorApplicationNode(
                            Not,
                            Arguments(
                                listOf(this.guard.deepCopy() as AtomicExpressionNode),
                                this.guard.sourceLocation
                            ),
                            this.guard.sourceLocation
                        ),
                        this.guard.sourceLocation
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
                                        this.guard.deepCopy() as AtomicExpressionNode,
                                        ReadNode(it)
                                    ),
                                    this.guard.sourceLocation
                                ),
                                this.guard.sourceLocation
                            )
                        } ?: this.guard.deepCopy() as AtomicExpressionNode,
                        this.guard.sourceLocation
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
                                    this.guard.sourceLocation
                                ),
                                this.guard.sourceLocation
                            )
                        } ?: ReadNode(negatedGuard),
                        this.guard.sourceLocation
                    )
                )

                newStatements.addAll(
                    this.thenBranch.asStraightLine(nameAnalysis, nameGenerator, pathGuard)
                )
                newStatements.addAll(
                    this.elseBranch.asStraightLine(nameAnalysis, nameGenerator, negatedPathGuard)
                )

                newStatements
            } else {
                assert(currentGuard == null)
                listOf(
                    IfNode(
                        this.guard.deepCopy() as AtomicExpressionNode,
                        this.thenBranch.mux(nameAnalysis, nameGenerator),
                        this.elseBranch.mux(nameAnalysis, nameGenerator),
                        this.sourceLocation
                    )
                )
            }
        }

        is InfiniteLoopNode -> {
            assert(currentGuard == null)
            listOf(
                InfiniteLoopNode(
                    this.body.mux(nameAnalysis, nameGenerator),
                    this.jumpLabel,
                    this.sourceLocation
                )
            )
        }

        is BreakNode -> listOf(this.deepCopy() as StatementNode)

        is AssertionNode -> listOf(this.deepCopy() as StatementNode)

        is BlockNode -> this.statements.flatMap { child ->
            if (child.canMux()) {
                child.asStraightLine(nameAnalysis, nameGenerator, currentGuard)
            } else {
                listOf(child.deepCopy() as StatementNode)
            }
        }
    }
