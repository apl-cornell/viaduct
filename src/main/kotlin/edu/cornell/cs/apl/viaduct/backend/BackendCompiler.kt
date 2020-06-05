package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.cli.print
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set as SetMethod
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Division
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
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.io.File

object BackendCompiler {
    fun compile(splitProgram: ProgramNode, output: File?) {
        val cppHostNameDecls: MutableList<CppDefineMacro> = mutableListOf()
        val cppProtocolNameDecls: MutableList<CppDefineMacro> = mutableListOf()
        val cppFunctionDecls: MutableList<CppFunctionDecl> = mutableListOf()
        val cppProcessObjectDecls: MutableList<CppVariableDecl> = mutableListOf()
        val cppRegisterProcessCalls: MutableList<CppCallStmt> = mutableListOf()

        var curHostId = 0
        var curProtocolId = 0
        for (decl: TopLevelDeclarationNode in splitProgram) {
            when (decl) {
                // create one function per process
                is ProcessDeclarationNode -> {
                    if (decl.protocol.value.protocolName != "Host") {
                        val processName: CppIdentifier = protocolProcessName(decl.protocol.value)
                        val functionName: CppIdentifier = protocolProcessFunctionName(decl.protocol.value)
                        val processObjectName = protocolProcessObjectName(decl.protocol.value)

                        val (processBody: CppBlock, processArguments: List<CppVariableDecl>) =
                            when (val protocolName = decl.protocol.value.protocolName) {
                                "Local", "Replication" -> {
                                    cppProcessObjectDecls.add(
                                        CppVariableDecl(
                                            if (protocolName == "Local") localProcessType else replicatedProcessType,
                                            processObjectName,
                                            listOf(
                                                CppReferenceRead(CppVariable(functionName))
                                            )
                                        )
                                    )

                                    Pair(
                                        compilePlaintextBlock(decl.body),
                                        listOf(CppVariableDecl(viaductProcessRuntimeType, runtimeIdent.name))
                                    )
                                }

                                "MPCWithAbort" -> {
                                    cppProcessObjectDecls.add(
                                        CppVariableDecl(
                                            mpcProcessType,
                                            processObjectName,
                                            listOf(
                                                CppReferenceRead(CppVariable(abyInfoIdent)),
                                                CppReferenceRead(CppVariable(functionName))
                                            )
                                        )
                                    )

                                    Pair(
                                        compileMPCBlock(decl.body, mutableMapOf()),
                                        listOf(
                                            CppVariableDecl(viaductProcessRuntimeType, runtimeIdent.name),
                                            CppVariableDecl(circuitType, circuitIdent.name)
                                        )
                                    )
                                }

                                else -> throw Error("backend compilation: unknown protocol $protocolName")
                            }

                        cppRegisterProcessCalls.add(runtimeRegisterProcess(decl.protocol.value))

                        val functionDecl = CppFunctionDecl(cppVoidType, functionName, processArguments, processBody)
                        cppFunctionDecls.add(functionDecl)
                        cppProtocolNameDecls.add(CppDefineMacro(processName, CppIntLiteral(curProtocolId)))
                        curProtocolId++
                    }
                }

                is HostDeclarationNode -> {
                    cppHostNameDecls.add(CppDefineMacro(decl.name.value.name, CppIntLiteral(curHostId)))
                    curHostId++
                }
            }
        }

        val cppTopLevelDecls: MutableList<CppTopLevelDeclaration> = mutableListOf()
        cppTopLevelDecls.addAll(cppHostNameDecls)
        cppTopLevelDecls.addAll(cppProtocolNameDecls)
        cppTopLevelDecls.addAll(cppFunctionDecls)

        // build start function
        // TODO: free allocated memory for arrays
        val startFunctionStmts: MutableList<CppStatement> = mutableListOf()
        startFunctionStmts.addAll(cppProcessObjectDecls)
        startFunctionStmts.addAll(cppRegisterProcessCalls)
        startFunctionStmts.add(runtimeRun())
        cppTopLevelDecls.add(
            CppFunctionDecl(
                cppVoidType,
                startFunctionIdent,
                listOf(
                    CppVariableDecl(viaductRuntimeType, runtimeIdent.name),
                    CppVariableDecl(abyInfoType, abyInfoIdent)
                ),
                CppBlock(startFunctionStmts)
            )
        )

        val cppProgram = CppProgram(cppTopLevelDecls)
        output.print(cppProgram)
        output.print(Document.forcedLineBreak)
        output.print(Document.forcedLineBreak)
    }

    private fun protocolProcessName(protocol: Protocol): String {
        val processName = StringBuilder()
        processName.append(protocol.protocolName)
        processName.append("_")
        for (host: Host in protocol.hosts) {
            processName.append("_")
            processName.append(host.name)
        }
        return processName.toString()
    }

    private fun protocolProcessFunctionName(protocol: Protocol): String {
        return "func_${protocolProcessName(protocol)}"
    }

    private fun protocolProcessObjectName(protocol: Protocol): String {
        return "proc_${protocolProcessName(protocol)}"
    }

    private val cppVoidType: CppTypeName = CppTypeName("void")
    private val cppIntType: CppTypeName = CppTypeName("int")
    private val viaductRuntimeType: CppType = CppReferenceType(CppTypeName("ViaductRuntime"))
    private val viaductProcessRuntimeType: CppType = CppReferenceType(CppTypeName("ViaductProcessRuntime"))

    private val localProcessType: CppType = CppTypeName("LocalProcess")
    private val replicatedProcessType: CppType = CppTypeName("ReplicatedProcess")
    private val mpcProcessType: CppType = CppTypeName("MPCProcess")

    private val runtimeIdent: CppVariable = CppVariable("runtime")
    private const val runtimeReceiveMethod: CppIdentifier = "receive"
    private const val runtimeSendMethod: CppIdentifier = "send"
    private const val runtimeHostInProtocolMethod: CppIdentifier = "isHostInProtocol"
    private const val runtimeInputMethod: CppIdentifier = "input"
    private const val runtimeOutputMethod: CppIdentifier = "output"
    private const val runtimeRegisterProcessMethod: CppIdentifier = "registerProcess"
    private const val runtimeRunMethod: CppIdentifier = "run"

    private val abyInfoType: CppType = CppTypeName("ABYInfo")
    private const val abyInfoIdent: CppIdentifier = "abyInfo"
    private const val startFunctionIdent: CppIdentifier = "start"

    private fun runtimeIfHostInProtocol(protocol: Protocol, thenBranch: CppBlock, elseBranch: CppBlock): CppIf {
        return CppIf(
            CppCallExpr(
                CppMethodCall(
                    CppReferenceRead(runtimeIdent),
                    runtimeHostInProtocolMethod,
                    listOf(
                        CppReferenceRead(CppVariable(protocolProcessName(protocol)))
                    )
                )
            ),
            thenBranch,
            elseBranch
        )
    }

    private fun runtimeSend(protocol: Protocol, expr: CppExpression): CppCallStmt {
        return CppCallStmt(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeSendMethod,
                listOf(
                    CppReferenceRead(CppVariable(protocolProcessName(protocol))),
                    expr
                )
            )
        )
    }

    private fun runtimeReceive(protocol: Protocol): CppCallExpr {
        return CppCallExpr(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeReceiveMethod,
                listOf(CppReferenceRead(CppVariable(protocolProcessName(protocol))))
            )
        )
    }

    private fun runtimeInput(): CppCallExpr {
        return CppCallExpr(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeInputMethod,
                listOf()
            )
        )
    }

    private fun runtimeOutput(output: CppExpression): CppCallStmt {
        return CppCallStmt(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeOutputMethod,
                listOf(output)
            )
        )
    }

    private fun runtimeRegisterProcess(protocol: Protocol): CppCallStmt {
        val processName: CppIdentifier = protocolProcessName(protocol)
        val processObjectName: CppIdentifier = protocolProcessObjectName(protocol)

        val cppArguments: MutableList<CppExpression> = mutableListOf()
        cppArguments.add(CppReferenceRead(CppVariable(processName)))
        cppArguments.add(CppReferenceRead(CppVariable(processObjectName)))
        cppArguments.add(CppIntLiteral(protocol.hosts.size))
        for (host: Host in protocol.hosts) {
            cppArguments.add(CppReferenceRead(CppVariable(host.name)))
        }

        return CppCallStmt(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeRegisterProcessMethod,
                cppArguments
            )
        )
    }

    private fun runtimeRun(): CppCallStmt {
        return CppCallStmt(
            CppMethodCall(
                CppReferenceRead(runtimeIdent),
                runtimeRunMethod,
                listOf()
            )
        )
    }

    // compile Local and Replicated processes

    private val cppOperatorMap: Map<Operator, (List<CppExpression>) -> CppExpression> = mapOf(
        Negation to { args -> CppUnaryOpExpr(CppUnaryOperator.NEGATION, args[0]) },
        Addition to { args -> CppBinaryOpExpr(CppBinaryOperator.ADD, args[0], args[1]) },
        Subtraction to { args -> CppBinaryOpExpr(CppBinaryOperator.SUBTRACT, args[0], args[1]) },
        Multiplication to { args -> CppBinaryOpExpr(CppBinaryOperator.MULTIPLY, args[0], args[1]) },
        Division to { args -> CppBinaryOpExpr(CppBinaryOperator.DIVIDE, args[0], args[1]) },
        Minimum to { args ->
            CppMux(CppBinaryOpExpr(CppBinaryOperator.LESS_THAN, args[0], args[1]), args[0], args[1])
        },
        Maximum to { args ->
            CppMux(CppBinaryOpExpr(CppBinaryOperator.LESS_THAN, args[0], args[1]), args[1], args[0])
        },
        Not to { args -> CppUnaryOpExpr(CppUnaryOperator.NOT, args[0]) },
        And to { args -> CppBinaryOpExpr(CppBinaryOperator.AND, args[0], args[1]) },
        Or to { args -> CppBinaryOpExpr(CppBinaryOperator.OR, args[0], args[1]) },
        EqualTo to { args -> CppBinaryOpExpr(CppBinaryOperator.EQUALS, args[0], args[1]) },
        LessThan to { args -> CppBinaryOpExpr(CppBinaryOperator.LESS_THAN, args[0], args[1]) },
        LessThanOrEqualTo to { args -> CppBinaryOpExpr(CppBinaryOperator.LT_EQUALS, args[0], args[1]) },
        Mux to { args -> CppMux(args[0], args[1], args[2]) }
    )

    private fun compilePlaintextExpr(expr: PureExpressionNode): CppExpression {
        return when (expr) {
            is LiteralNode -> {
                when (val exprVal = expr.value) {
                    is IntegerValue -> CppIntLiteral(exprVal.value)
                    is BooleanValue -> CppIntLiteral(if (exprVal.value) 1 else 0)
                    else -> throw Error("backend compilation: unknown value type")
                }
            }

            is ReadNode -> CppReferenceRead(CppVariable(expr.temporary.value.name))

            is OperatorApplicationNode -> {
                val cppArguments: List<CppExpression> = expr.arguments.map { compilePlaintextExpr(it) }
                cppOperatorMap[expr.operator]?.let { it(cppArguments) }
                    ?: throw Error("backend compilation: cannot compile operator")
            }

            is QueryNode -> {
                when (expr.query.value) {
                    is Get -> {
                        if (expr.arguments.size == 0) { // variable read
                            CppReferenceRead(CppVariable(expr.variable.value.name))
                        } else { // array read
                            CppReferenceRead(
                                CppArrayIndex(
                                    CppVariable(expr.variable.value.name),
                                    compilePlaintextExpr(expr.arguments[0])
                                )
                            )
                        }
                    }

                    else -> throw Error("backend compilation: unknown query ${expr.query.value}")
                }
            }

            // downgrade nodes have no computational content! compile them away

            is DeclassificationNode -> compilePlaintextExpr(expr.expression)
            is EndorsementNode -> compilePlaintextExpr(expr.expression)
        }
    }

    private fun compilePlaintextBlock(block: BlockNode): CppBlock {
        val cppChildrenStmts: MutableList<CppStatement> = mutableListOf()
        for (childStmt: StatementNode in block) {
            cppChildrenStmts.addAll(compilePlaintextStmt(childStmt))
        }
        return CppBlock(cppChildrenStmts)
    }

    private fun compilePlaintextStmt(stmt: StatementNode): List<CppStatement> {
        return when (stmt) {
            is LetNode -> {
                when (val rhs = stmt.value) {
                    /* TODO: Repl() processes must broadcast local values received to Repl() processes in other hosts */
                    is ReceiveNode -> {
                        listOf(
                            CppVariableDecl(cppIntType, stmt.temporary.value.name),
                            runtimeIfHostInProtocol(
                                rhs.protocol.value,

                                // host is in sending protocol, actually receive
                                CppBlock(
                                    listOf(
                                        CppAssignment(
                                            CppVariable(stmt.temporary.value.name),
                                            runtimeReceive(rhs.protocol.value)
                                        )
                                    )
                                ),

                                // host not in sending protocol, don't receive
                                CppBlock(listOf())
                            )
                        )
                    }

                    is InputNode -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                cppIntType,
                                stmt.temporary.value.name,
                                runtimeInput()
                            )
                        )
                    }

                    is PureExpressionNode -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                cppIntType,
                                stmt.temporary.value.name,
                                compilePlaintextExpr(rhs)
                            )
                        )
                    }
                }
            }

            is DeclarationNode -> {
                when (stmt.className.value) {
                    ImmutableCell -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                cppIntType,
                                stmt.variable.value.name,
                                compilePlaintextExpr(stmt.arguments[0])
                            )
                        )
                    }

                    MutableCell -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                cppIntType,
                                stmt.variable.value.name,
                                compilePlaintextExpr(stmt.arguments[0])
                            )
                        )
                    }

                    Vector -> {
                        listOf(
                            CppArrayDecl(
                                cppIntType,
                                stmt.variable.value.name,
                                compilePlaintextExpr(stmt.arguments[0])
                            )
                        )
                    }

                    else -> throw Error("backend compilation: unknown variable declaration type")
                }
            }

            is UpdateNode -> {
                when (val update = stmt.update.value) {
                    is SetMethod -> {
                        if (stmt.arguments.size == 1) { // variable update
                            val cppRhs: CppExpression = compilePlaintextExpr(stmt.arguments[0])
                            listOf(
                                CppAssignment(CppVariable(stmt.variable.value.name), cppRhs)
                            )
                        } else { // array update
                            val cppIndex: CppExpression = compilePlaintextExpr(stmt.arguments[0])
                            val cppRhs: CppExpression = compilePlaintextExpr(stmt.arguments[1])
                            listOf(
                                CppAssignment(CppArrayIndex(CppVariable(stmt.variable.value.name), cppIndex), cppRhs)
                            )
                        }
                    }

                    is Modify -> {
                        if (stmt.arguments.size == 1) { // variable update
                            val cppVar = CppVariable(stmt.variable.value.name)
                            val cppRhs: CppExpression = compilePlaintextExpr(stmt.arguments[0])
                            val cppFinalRhs: CppExpression =
                                cppOperatorMap[update.operator]?.let {
                                    it(listOf(CppReferenceRead(cppVar), cppRhs))
                                } ?: throw Error("backend compilation: unknown operator")

                            listOf(
                                CppAssignment(cppVar, cppFinalRhs)
                            )
                        } else { // array update
                            val cppVar = CppVariable(stmt.variable.value.name)
                            val cppIndex = compilePlaintextExpr(stmt.arguments[0])
                            val cppArrayIndex = CppArrayIndex(cppVar, cppIndex)
                            val cppRhs = compilePlaintextExpr(stmt.arguments[1])
                            val cppFinalRhs: CppExpression =
                                cppOperatorMap[update.operator]?.let {
                                    it(listOf(CppReferenceRead(cppArrayIndex), cppRhs))
                                } ?: throw Error("backend compilation: unknown operator")

                            listOf(
                                CppAssignment(cppArrayIndex, cppFinalRhs)
                            )
                        }
                    }

                    else -> throw Error("backend compilation: unknown update operation")
                }
            }

            is OutputNode -> {
                listOf(
                    runtimeOutput(compilePlaintextExpr(stmt.message))
                )
            }

            is SendNode -> {
                listOf(
                    runtimeIfHostInProtocol(
                        stmt.protocol.value,

                        // recipient protocol is in host; actually send value
                        CppBlock(
                            listOf(
                                runtimeSend(stmt.protocol.value, compilePlaintextExpr(stmt.message))
                            )
                        ),

                        // recipient protocol is not in host; don't do anything
                        CppBlock(listOf())
                    )
                )
            }

            is IfNode -> {
                val cppGuard: CppExpression = compilePlaintextExpr(stmt.guard)
                val cppThenBranch: CppBlock = compilePlaintextBlock(stmt.thenBranch)
                val cppElseBranch: CppBlock = compilePlaintextBlock(stmt.elseBranch)
                listOf(CppIf(cppGuard, cppThenBranch, cppElseBranch))
            }

            is InfiniteLoopNode -> {
                val cppBody: CppBlock = compilePlaintextBlock(stmt.body)
                listOf(CppWhileLoop(CppIntLiteral(1), cppBody))
            }

            is BreakNode -> listOf(CppBreak)

            is AssertionNode -> listOf()

            is BlockNode -> listOf(compilePlaintextBlock(stmt))
        }
    }

    // MPC process compilation to ABY

    private val circuitType: CppType = CppReferenceType(CppTypeName("CachedCircuit"))
    private val circuitGateType: CppType = CppPointerType(CppTypeName("CircuitGate"))
    private val secretShareType: CppType = CppPointerType(CppTypeName("share"))

    private val circuitIdent: CppVariable = CppVariable("circuit")
    private const val resetMethod: CppIdentifier = "Reset"
    private const val buildCircuitMethod: CppIdentifier = "BuildCircuit"
    private const val execCircuitMethod: CppIdentifier = "ExecCircuit"
    private val getClearValueMethod: CppIdentifier = "get_clear_value<${cppIntType.name}>"

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

    private fun buildCircuitGate(gate: CircuitGate, vararg arguments: CppExpression): CppExpression =
        CppCallExpr(
            CppMethodCall(
                CppReferenceRead(circuitIdent),
                gate.gateMethod,
                listOf(*arguments)
            )
        )

    private val circuitOperatorMap: Map<Operator, (List<CppExpression>) -> CppExpression> = mapOf(
        Negation to { args ->
            buildCircuitGate(
                CircuitGate.SUB_GATE,
                buildCircuitGate(CircuitGate.CONST_GATE, CppIntLiteral(0)),
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

    private fun compileMPCExpr(
        expr: PureExpressionNode,
        shareMap: Map<Temporary, CppVariable>,
        useShares: Boolean = true
    ): CppExpression {
        return when (expr) {
            is LiteralNode -> {
                when (val exprVal: Value = expr.value) {
                    is IntegerValue -> {
                        buildCircuitGate(CircuitGate.CONST_GATE, CppIntLiteral(exprVal.value))
                    }
                    is BooleanValue -> {
                        buildCircuitGate(CircuitGate.CONST_GATE, CppIntLiteral(if (exprVal.value) 1 else 0))
                    }
                    else -> throw Error("backend compilation: unknown value type")
                }
            }

            is ReadNode -> {
                if (useShares) { // the input will be used for computation, so use its input gate / share counterpart
                    shareMap[expr.temporary.value]?.let { CppReferenceRead(it) }
                        ?: throw Error(
                            "backend compilation: no input gate found for temporary ${expr.temporary.value.name}")
                } else { // the input will be used for control flow or array indexing, so use its plaintext version
                    CppReferenceRead(CppVariable(expr.temporary.value.name))
                }
            }

            is QueryNode -> {
                when (expr.query.value) {
                    is Get -> {
                        if (expr.arguments.size == 0) { // variable read
                            CppReferenceRead(CppVariable(expr.variable.value.name))
                        } else { // array read
                            val cppIndex: CppExpression = compileMPCExpr(expr.arguments[0], shareMap, useShares = false)
                            CppReferenceRead(
                                CppArrayIndex(CppVariable(expr.variable.value.name), cppIndex)
                            )
                        }
                    }

                    else -> throw Error("backend compilation: unknown query ${expr.query.value}")
                }
            }

            is OperatorApplicationNode -> {
                val cppArguments: List<CppExpression> = expr.arguments.map { compileMPCExpr(it, shareMap) }
                circuitOperatorMap[expr.operator]?.let { it(cppArguments) }
                    ?: throw Error("backend compilation: cannot compile operator")
            }

            // downgrading nodes have no computational content!
            // they get compiled away

            is DeclassificationNode -> compileMPCExpr(expr.expression, shareMap)

            is EndorsementNode -> compileMPCExpr(expr.expression, shareMap)
        }
    }

    private fun compileMPCBlock(block: BlockNode, shareMap: MutableMap<Temporary, CppVariable>): CppBlock {
        val cppChildrenStmts: MutableList<CppStatement> = mutableListOf()
        for (childStmt: StatementNode in block) {
            cppChildrenStmts.addAll(compileMPCStmt(childStmt, shareMap))
        }
        return CppBlock(cppChildrenStmts)
    }

    private fun compileMPCStmt(stmt: StatementNode, shareMap: MutableMap<Temporary, CppVariable>): List<CppStatement> {
        return when (stmt) {
            is LetNode -> {
                when (val rhs: ExpressionNode = stmt.value) {
                    // receive input from another process
                    is ReceiveNode -> {
                        val inputName = CppVariable(stmt.temporary.value.name)
                        val shareName = CppVariable("${inputName.name}__share")

                        shareMap[stmt.temporary.value] = shareName
                        listOf(
                            CppVariableDecl(cppIntType, inputName.name),
                            CppVariableDecl(circuitGateType, shareName.name),
                            runtimeIfHostInProtocol(
                                rhs.protocol.value,

                                // receive input and create input gate
                                CppBlock(
                                    listOf(
                                        CppAssignment(
                                            inputName,
                                            runtimeReceive(rhs.protocol.value)
                                        ),
                                        CppAssignment(
                                            shareName,
                                            buildCircuitGate(
                                                CircuitGate.IN_GATE,
                                                CppReferenceRead(inputName)
                                            )
                                        )
                                    )
                                ),

                                // not receiving input; create dummy input gate
                                CppBlock(
                                    listOf(
                                        CppAssignment(
                                            shareName,
                                            buildCircuitGate(CircuitGate.DUMMY_IN_GATE)
                                        )
                                    )
                                )
                            )
                        )
                    }

                    // execute circuit
                    is InputNode -> throw Error("backend compilation: no input possible in MPC process")

                    is PureExpressionNode -> {
                        shareMap[stmt.temporary.value] = CppVariable(stmt.temporary.value.name)
                        listOf(
                            CppVariableDeclAndAssignment(
                                circuitGateType,
                                stmt.temporary.value.name,
                                compileMPCExpr(rhs, shareMap)
                            )
                        )
                    }
                }
            }

            is DeclarationNode -> {
                when (stmt.className.value) {
                    ImmutableCell -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                circuitGateType,
                                stmt.variable.value.name,
                                compileMPCExpr(stmt.arguments[0], shareMap)
                            )
                        )
                    }

                    MutableCell -> {
                        listOf(
                            CppVariableDeclAndAssignment(
                                circuitGateType,
                                stmt.variable.value.name,
                                compileMPCExpr(stmt.arguments[0], shareMap)
                            )
                        )
                    }

                    Vector -> {
                        listOf(
                            CppArrayDecl(
                                circuitGateType,
                                stmt.variable.value.name,
                                compileMPCExpr(stmt.arguments[0], shareMap, useShares = false)
                            )
                        )
                    }

                    else -> throw Error("backend compilation: unknown variable declaration type")
                }
            }

            is UpdateNode -> {
                when (val update = stmt.update.value) {
                    is SetMethod -> {
                        if (stmt.arguments.size == 1) { // variable update
                            val cppRhs: CppExpression = compileMPCExpr(stmt.arguments[0], shareMap)
                            listOf(
                                CppAssignment(CppVariable(stmt.variable.value.name), cppRhs)
                            )
                        } else { // array update
                            val cppIndex: CppExpression = compileMPCExpr(stmt.arguments[0], shareMap, useShares = false)
                            val cppRhs: CppExpression = compileMPCExpr(stmt.arguments[1], shareMap)
                            listOf(
                                CppAssignment(CppArrayIndex(CppVariable(stmt.variable.value.name), cppIndex), cppRhs)
                            )
                        }
                    }

                    is Modify -> {
                        if (stmt.arguments.size == 1) { // variable update
                            val cppVar = CppVariable(stmt.variable.value.name)
                            val cppRhs: CppExpression = compileMPCExpr(stmt.arguments[0], shareMap)
                            val cppFinalRhs: CppExpression =
                                circuitOperatorMap[update.operator]?.let {
                                    it(listOf(CppReferenceRead(cppVar), cppRhs))
                                } ?: throw Error("backend compilation: unknown operator")

                            listOf(
                                CppAssignment(cppVar, cppFinalRhs)
                            )
                        } else { // array update
                            val cppVar = CppVariable(stmt.variable.value.name)
                            val cppIndex = compileMPCExpr(stmt.arguments[0], shareMap, useShares = false)
                            val cppArrayIndex = CppArrayIndex(cppVar, cppIndex)
                            val cppRhs = compileMPCExpr(stmt.arguments[1], shareMap)
                            val cppFinalRhs: CppExpression =
                                circuitOperatorMap[update.operator]?.let {
                                    it(listOf(CppReferenceRead(cppArrayIndex), cppRhs))
                                } ?: throw Error("backend compilation: unknown operator")

                            listOf(
                                CppAssignment(cppArrayIndex, cppFinalRhs)
                            )
                        }
                    }

                    else -> throw Error("backend compilation: unknown update operation")
                }
            }

            is OutputNode -> throw Error("backend compilation: no output possible in MPC process")

            is SendNode -> {
                val cppSendStmts: List<CppStatement> =
                    when (val sendValNode: AtomicExpressionNode = stmt.message) {
                        is LiteralNode -> {
                            val cppSendVal: Int =
                                when (val sendVal = sendValNode.value) {
                                    is BooleanValue -> if (sendVal.value) 1 else 0
                                    is IntegerValue -> sendVal.value
                                    else -> throw Error("backend compilation: unknown value type")
                                }

                            listOf(
                                runtimeSend(stmt.protocol.value, CppIntLiteral(cppSendVal))
                            )
                        }

                        // execute circuit!
                        is ReadNode -> {
                            val varName = sendValNode.temporary.value.name
                            val shareName: CppIdentifier = varName + "__out"
                            val clearValueName: CppIdentifier = shareName + "__clear"

                            listOf(
                                // circuit.Reset()
                                CppCallStmt(
                                    CppMethodCall(CppReferenceRead(circuitIdent), resetMethod, listOf())
                                ),

                                // share* s_out = circuit.BuildCircuit(s_sum)
                                CppVariableDeclAndAssignment(
                                    secretShareType,
                                    shareName,
                                    CppCallExpr(
                                        CppMethodCall(
                                            CppReferenceRead(circuitIdent),
                                            buildCircuitMethod,
                                            listOf(CppReferenceRead(CppVariable(varName)))
                                        )
                                    )
                                ),

                                // circuit.ExecCircuit()
                                CppCallStmt(
                                    CppMethodCall(
                                        CppReferenceRead(circuitIdent),
                                        execCircuitMethod,
                                        listOf()
                                    )
                                ),

                                // int output = (*s_out).get_clear_value<int>()
                                CppVariableDeclAndAssignment(
                                    cppIntType,
                                    clearValueName,
                                    CppCallExpr(
                                        CppMethodCall(
                                            CppDeref(CppReferenceRead(CppVariable(shareName))),
                                            getClearValueMethod,
                                            listOf()
                                        )
                                    )
                                ),

                                // runtime.send(RECEIVER, output)
                                runtimeSend(stmt.protocol.value, CppReferenceRead(CppVariable(clearValueName)))
                            )
                        }
                    }

                listOf(
                    runtimeIfHostInProtocol(stmt.protocol.value, CppBlock(cppSendStmts), CppBlock(listOf()))
                )
            }

            is IfNode -> {
                // don't convert received values into input gates
                val cppGuard: CppExpression = compileMPCExpr(stmt.guard, shareMap, false)
                val cppThenBranch: CppBlock = compileMPCBlock(stmt.thenBranch, shareMap)
                val cppElseBranch: CppBlock = compileMPCBlock(stmt.elseBranch, shareMap)
                listOf(CppIf(cppGuard, cppThenBranch, cppElseBranch))
            }

            is InfiniteLoopNode -> {
                val cppBody: CppBlock = compileMPCBlock(stmt.body, shareMap)
                listOf(CppWhileLoop(CppIntLiteral(1), cppBody))
            }

            is BreakNode -> listOf(CppBreak)

            is BlockNode -> listOf(compileMPCBlock(stmt, shareMap))

            is AssertionNode -> listOf()
        }
    }
}
