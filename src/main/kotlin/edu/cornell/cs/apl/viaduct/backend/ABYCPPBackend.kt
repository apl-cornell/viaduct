package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set as SetMethod
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.persistentListOf

class ABYCPPBackend(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) : CppBuilder(), CppBackend {

    private val abyInfoType = CppTypeName("ABYInfo")

    private val abyProcessType = CppTypeName("MPCProcess")

    private val abyCachedCircuitType =
        CppReferenceType(CppTypeName("CachedCircuit"))

    private val abyCircuitGateType = CppPointerType(
        CppTypeName("CircuitGate")
    )
    private val abySecretShareType = CppPointerType(
        CppTypeName("share")
    )

    private val abyInfoIdent = "abyInfo"
    private val circuitIdent = "circuit"
    private val circuitResetMethod = "Reset"
    private val circuitBuildCircuitMethod = "BuildCircuit"
    private val circuitExecCircuitMethod = "ExecCircuit"
    private val circuitGetClearValueMethod = "get_clear_value<${cppIntType.name}>"

    enum class CircuitGate(val gateMethod: CppIdentifier) {
        IN_GATE("PutINGate"),
        DUMMY_IN_GATE("PutDummyINGate"),
        CONST_GATE("PutCONSTGate"),
        AND_GATE("PutANDGate"),
        OR_GATE("PutORGate"),
        XOR_GATE("PutXORGate"),
        ADD_GATE("PutADDGate"),
        MUL_GATE("PutMULGate"),
        SUB_GATE("PutSUBGate"),
        GT_GATE("PutGTGate"),
        MUX_GATE("PutMUXGate"),
        INV_GATE("PutINVGate"),
    }

    override val supportedProtocols: Set<String>
        get() = setOf(ABY.protocolName)

    override val extraStartArguments: List<CppFormalDecl>
        get() = listOf(CppFormalDecl(abyInfoType, abyInfoIdent))

    override fun extraFunctionArguments(protocol: Protocol): List<CppFormalDecl> =
        listOf(CppFormalDecl(abyCachedCircuitType, circuitIdent))

    override fun buildProcessObject(
        protocol: Protocol,
        procName: CppIdentifier,
        funcName: CppIdentifier
    ): List<CppStatement> =
        listOf(
            CppVariableDecl(
                type = abyProcessType,
                name = procName,
                arguments = listOf(read(abyInfoIdent), read(funcName))
            )
        )

    override fun compile(block: BlockNode, protocol: Protocol, host: Host): CppBlock {
        return when (protocol) {
            is ABY -> compileMPCBlock(block, host, mutableMapOf())
            else -> throw Error("backend compilation: protocol ${protocol.protocolName} unsupported by ABY backend")
        }
    }

    private fun buildCircuitGate(gate: CircuitGate, vararg arguments: CppExpression): CppExpression =
        methodCallExpr(
            read(circuitIdent),
            gate.gateMethod,
            *arguments
        )

    private val circuitOperatorMap: Map<Operator, (List<CppExpression>) -> CppExpression> = mapOf(
        Negation to { args ->
            buildCircuitGate(
                CircuitGate.SUB_GATE,
                buildCircuitGate(
                    CircuitGate.CONST_GATE,
                    CppIntLiteral(0)
                ),
                args[0]
            )
        },
        Addition to { args -> buildCircuitGate(CircuitGate.ADD_GATE, args[0], args[1]) },
        Subtraction to { args -> buildCircuitGate(CircuitGate.SUB_GATE, args[0], args[1]) },
        Multiplication to { args -> buildCircuitGate(CircuitGate.MUL_GATE, args[0], args[1]) },
        // Division to { args -> buildCircuitGate(CircuitGate.DIV_GATE, args[0], args[1]) },
        Minimum to { args ->
            buildCircuitGate(
                CircuitGate.MUX_GATE,
                buildCircuitGate(CircuitGate.GT_GATE, args[0], args[1]),
                args[1],
                args[0]
            )
        },
        Maximum to { args ->
            buildCircuitGate(
                CircuitGate.MUX_GATE,
                buildCircuitGate(CircuitGate.GT_GATE, args[0], args[1]),
                args[0],
                args[1]
            )
        },
        // TODO: check if INV gate is actually NOT
        Not to { args -> buildCircuitGate(CircuitGate.INV_GATE, args[0]) },
        And to { args -> buildCircuitGate(CircuitGate.AND_GATE, args[0], args[1]) },
        Or to { args -> buildCircuitGate(CircuitGate.OR_GATE, args[0], args[1]) },
        EqualTo to { args -> // (a == b) <--> (a <= b && b <= a)
            buildCircuitGate(
                CircuitGate.AND_GATE,
                buildCircuitGate(
                    CircuitGate.INV_GATE,
                    buildCircuitGate(CircuitGate.GT_GATE, args[0], args[1])
                ),
                buildCircuitGate(
                    CircuitGate.INV_GATE,
                    buildCircuitGate(CircuitGate.GT_GATE, args[1], args[0])
                )
            )
        },
        LessThan to { args -> buildCircuitGate(CircuitGate.GT_GATE, args[1], args[0]) },
        LessThanOrEqualTo to { args ->
            buildCircuitGate(
                CircuitGate.INV_GATE,
                buildCircuitGate(CircuitGate.GT_GATE, args[0], args[1])
            )
        },
        Mux to { args -> buildCircuitGate(CircuitGate.MUX_GATE, args[0], args[1], args[2]) }
    )

    // helpers

    private fun abyDummyIn() = buildCircuitGate(CircuitGate.DUMMY_IN_GATE)

    private fun abyIn(input: CppExpression) = buildCircuitGate(
        CircuitGate.IN_GATE, input
    )

    private fun abyConst(value: Int) = buildCircuitGate(
        CircuitGate.CONST_GATE,
        CppIntLiteral(value)
    )

    private fun abyExecCircuit(
        outputGate: CppIdentifier,
        shareVariable: CppIdentifier,
        clearVariable: CppIdentifier
    ): List<CppStatement> =
        listOf(
            // circuit.Reset()
            methodCallStmt(read(circuitIdent), circuitResetMethod),

            // share* s_out = circuit.BuildCircuit(s_sum)
            CppVariableDeclAndAssignment(
                abySecretShareType,
                shareVariable,
                methodCallExpr(read(circuitIdent), circuitBuildCircuitMethod, read(outputGate))
            ),

            // circuit.ExecCircuit()
            methodCallStmt(read(circuitIdent), circuitExecCircuitMethod),

            // int output = (*s_out).get_clear_value<int>()
            CppVariableDeclAndAssignment(
                cppIntType,
                clearVariable,
                methodCallExpr(
                    CppDeref(read(shareVariable)),
                    circuitGetClearValueMethod
                )
            )
        )

    private fun compileOperator(operator: Operator, arguments: List<CppExpression>): CppExpression =
        circuitOperatorMap[operator]?.let { gateBuilder: (List<CppExpression>) -> CppExpression ->
            gateBuilder(arguments)
        } ?: throw Error("backend compilation: ABY backend cannot compile operator $operator")

    private fun compileOperator(operator: Operator, vararg arguments: CppExpression): CppExpression =
        compileOperator(operator, listOf(*arguments))

    private fun compileMPCExpr(
        expr: PureExpressionNode,
        shareMap: MutableMap<Temporary, CppIdentifier>,
        useShares: Boolean = true
    ): CppExpression {
        return when (expr) {
            is LiteralNode -> {
                val literal: Int =
                    when (val value: Value = expr.value) {
                        is IntegerValue -> value.value

                        is BooleanValue -> if (value.value) 1 else 0

                        else -> throw Error("backend compilation: unknown value type")
                    }

                buildCircuitGate(
                    CircuitGate.CONST_GATE,
                    CppIntLiteral(literal)
                )
            }

            is ReadNode -> {
                if (useShares) {
                    shareMap[expr.temporary.value]?.let { ident -> read(ident) }
                        ?: throw Error("backend compilation: no secret share found for ${expr.temporary.value}")
                } else {
                    read(expr.temporary.value.name)
                }
            }

            is OperatorApplicationNode -> {
                compileOperator(expr.operator, expr.arguments.map { compileMPCExpr(it, shareMap) })
            }

            is QueryNode -> {
                when (expr.query.value) {
                    is Get -> {
                        val type: ObjectType = typeAnalysis.type(nameAnalysis.declaration(expr))

                        if (type is VectorType) {
                            readArray(
                                expr.variable.value.name,
                                compileMPCExpr(expr.arguments[0], shareMap, useShares = false)
                            )
                        } else {
                            read(expr.variable.value.name)
                        }
                    }

                    else -> throw Error("backend compilation: unknown query ${expr.query.value.name}")
                }
            }

            // downgrades have no computational content, so compile them away

            is DeclassificationNode -> compileMPCExpr(expr.expression, shareMap)

            is EndorsementNode -> compileMPCExpr(expr.expression, shareMap)
        }
    }

    private fun compileMPCBlock(
        block: BlockNode,
        host: Host,
        shareMap: MutableMap<Temporary, CppIdentifier>
    ): CppBlock {
        val childStmts: MutableList<CppStatement> = mutableListOf()
        val arrayDecls: MutableList<DeclarationNode> = mutableListOf()

        for (stmt: StatementNode in block) {
            childStmts.addAll(compileMPCStmt(stmt, host, shareMap, arrayDecls))
        }

        for (arrayDecl: DeclarationNode in arrayDecls) {
            childStmts.add(deleteArray(arrayDecl.variable.value.name))
        }

        return CppBlock(childStmts)
    }

    private fun compileMPCStmt(
        stmt: StatementNode,
        host: Host,
        shareMap: MutableMap<Temporary, CppIdentifier>,
        arrayDecls: MutableList<DeclarationNode>
    ): List<CppStatement> {
        return when (stmt) {
            is LetNode -> {
                val tmp: Temporary = stmt.temporary.value

                when (val rhs: ExpressionNode = stmt.value) {
                    is ReceiveNode -> {
                        val shareName = "${tmp.name}__share"
                        shareMap[stmt.temporary.value] = shareName

                        if (rhs.protocol.value.hosts.contains(host)) {
                            listOf(
                                receive(
                                    variable = tmp.name,
                                    type = cppIntType,
                                    senderProtocol = rhs.protocol.value,
                                    senderHost = host
                                ),
                                declare(
                                    variable = shareName,
                                    type = abyCircuitGateType,
                                    initVal = abyIn(read(tmp.name))
                                )
                            )
                        } else {
                            listOf(
                                declare(
                                    variable = shareName,
                                    type = abyCircuitGateType,
                                    initVal = abyDummyIn()
                                )
                            )
                        }
                    }

                    is InputNode -> throw Error("backend compilation: no input possible in MPC process")

                    is PureExpressionNode -> {
                        shareMap[tmp] = tmp.name
                        listOf(
                            declare(
                                variable = tmp.name,
                                type = abyCircuitGateType,
                                initVal = compileMPCExpr(rhs, shareMap)
                            )
                        )
                    }
                }
            }

            is DeclarationNode -> {
                val type: ObjectType = typeAnalysis.type(stmt)

                listOf(
                    when (type) {
                        is VectorType -> {
                            arrayDecls.add(stmt)
                            declareArray(
                                variable = stmt.variable.value.name,
                                elementType = abyCircuitGateType,
                                length = compileMPCExpr(stmt.arguments[0], shareMap, useShares = false)
                            )
                        }

                        is ImmutableCellType, is MutableCellType -> {
                            declare(
                                variable = stmt.variable.value.name,
                                type = abyCircuitGateType,
                                initVal = compileMPCExpr(stmt.arguments[0], shareMap)
                            )
                        }

                        else -> throw Error("backend compilation: unknown object type ${type.className}")
                    }
                )
            }

            is UpdateNode -> {
                val type: ObjectType = typeAnalysis.type(nameAnalysis.declaration(stmt))

                listOf(
                    when (type) {
                        is VectorType -> {
                            val arrayName: CppIdentifier = stmt.variable.value.name
                            val index: CppExpression = compileMPCExpr(stmt.arguments[0], shareMap, useShares = false)
                            val rhs: CppExpression = compileMPCExpr(stmt.arguments[1], shareMap)
                            val finalRhs: CppExpression =
                                when (val update: UpdateName = stmt.update.value) {
                                    is SetMethod -> rhs

                                    is Modify -> compileOperator(update.operator, readArray(arrayName, index), rhs)

                                    else -> throw Error("backend compilation: unknown update method $update")
                                }

                            updateArray(arrayName, index, finalRhs)
                        }

                        is MutableCellType -> {
                            val varName: CppIdentifier = stmt.variable.value.name
                            val rhs: CppExpression = compileMPCExpr(stmt.arguments[0], shareMap)
                            val finalRhs: CppExpression =
                                when (val update: UpdateName = stmt.update.value) {
                                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> rhs

                                    is Modify -> {
                                        compileOperator(update.operator, read(varName), rhs)
                                    }

                                    else -> throw Error("backend compilation: unknown update method $update")
                                }

                            update(varName, finalRhs)
                        }

                        else -> throw Error("backend compilation: unknown object type $type in update")
                    }
                )
            }

            is OutputNode -> {
                listOf(
                    output(compileMPCExpr(stmt.message, shareMap))
                )
            }

            is SendNode -> {
                if (stmt.protocol.value.hosts.contains(host)) {
                    when (val message: AtomicExpressionNode = stmt.message) {
                        is LiteralNode -> {
                            listOf(
                                send(
                                    recvProtocol = stmt.protocol.value,
                                    recvHost = host,
                                    message = compileMPCExpr(stmt.message, shareMap)
                                )
                            )
                        }

                        is ReadNode -> {
                            val outName: CppIdentifier = message.temporary.value.name
                            val shareName: CppIdentifier = "${outName}__share"
                            val clearName: CppIdentifier = "${outName}__clear"
                            persistentListOf<CppStatement>()
                                .addAll(abyExecCircuit(outName, shareName, clearName))
                                .add(
                                    send(
                                        recvProtocol = stmt.protocol.value,
                                        recvHost = host,
                                        message = read(clearName)
                                    )
                                )
                        }
                    }
                } else {
                    listOf()
                }
            }

            is IfNode -> {
                listOf(
                    CppIf(
                        guard = compileMPCExpr(stmt.guard, shareMap, useShares = false),
                        thenBranch = compileMPCBlock(stmt.thenBranch, host, shareMap),
                        elseBranch = compileMPCBlock(stmt.elseBranch, host, shareMap)
                    )
                )
            }

            is InfiniteLoopNode -> {
                listOf(loop(compileMPCBlock(stmt.body, host, shareMap)))
            }

            is BreakNode -> listOf(loopBreak())

            is AssertionNode -> listOf()

            // this shouldn't be called
            is BlockNode -> listOf(compileMPCBlock(stmt, host, shareMap))
        }
    }
}
