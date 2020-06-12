package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set as SetMethod
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
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
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

private typealias RecvCompiler = (Temporary, ReceiveNode, Protocol, Host) -> List<CppStatement>
private typealias SendCompiler = (SendNode, Protocol, Host) -> List<CppStatement>

/** Compile Viaduct programs by translating them directly into C++. */
open class PlaintextCppBackend(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) : CppBuilder(), CppBackend {

    private val localProcessType = CppTypeName("LocalProcess")
    private val replicationProcessType = CppTypeName("ReplicatedProcess")

    override val supportedProtocols: Set<String>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override val extraStartArguments: List<CppFormalDecl>
        get() = listOf()

    override fun extraFunctionArguments(protocol: Protocol): List<CppFormalDecl> = listOf()

    override fun buildProcessObject(
        protocol: Protocol,
        procName: CppIdentifier,
        funcName: CppIdentifier
    ): List<CppStatement> {
        return when (protocol) {
            is Local -> {
                listOf(
                    CppVariableDecl(
                        type = localProcessType,
                        name = procName,
                        arguments = listOf(read(funcName))
                    )
                )
            }

            is Replication -> {
                listOf(
                    CppVariableDecl(
                        type = replicationProcessType,
                        name = procName,
                        arguments = listOf(read(funcName))
                    )
                )
            }

            else ->
                throw Error("backend compilation: protocol ${protocol.protocolName} unsupported by Plaintext backend")
        }
    }

    override fun compile(block: BlockNode, protocol: Protocol, host: Host): CppBlock {
        return when (protocol) {
            is Local ->
                compilePlaintextBlock(block, protocol, host, ::compileLocalReceive, ::compileLocalSend)

            is Replication ->
                compilePlaintextBlock(block, protocol, host, ::compileReplicationReceive, ::compileReplicationSend)

            else ->
                throw Error("backend compilation: protocol ${protocol.protocolName} unsupported by Plaintext backend")
        }
    }

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

    private fun compilePlaintextBlock(
        block: BlockNode,
        protocol: Protocol,
        host: Host,
        recvCompiler: RecvCompiler,
        sendCompiler: SendCompiler
    ): CppBlock {
        val childStmts: MutableList<CppStatement> = mutableListOf()
        val arrayDecls: MutableList<DeclarationNode> = mutableListOf()

        for (stmt: StatementNode in block) {
            childStmts.addAll(
                compilePlaintextStmt(stmt, protocol, host, arrayDecls, recvCompiler, sendCompiler)
            )
        }

        for (arrayDecl: DeclarationNode in arrayDecls) {
            childStmts.add(deleteArray(arrayDecl.variable.value.name))
        }

        return CppBlock(childStmts)
    }

    private fun compileOperator(operator: Operator, arguments: List<CppExpression>): CppExpression =
        cppOperatorMap[operator]?.let { exprBuilder ->
            exprBuilder(arguments)
        } ?: throw Error("backend compilation: Plaintext backend cannot compile operator $operator")

    private fun compileOperator(operator: Operator, vararg arguments: CppExpression): CppExpression =
        compileOperator(operator, listOf(*arguments))

    private fun compilePlaintextExpr(expr: PureExpressionNode): CppExpression {
        return when (expr) {
            is LiteralNode ->
                when (val value: Value = expr.value) {
                    is IntegerValue -> CppIntLiteral(value.value)

                    is BooleanValue -> CppIntLiteral(if (value.value) 1 else 0)

                    else -> throw Error("backend compilation: unknown value type")
                }

            is ReadNode -> read(expr.temporary.value.name)

            is OperatorApplicationNode ->
                compileOperator(expr.operator, expr.arguments.map { arg -> compilePlaintextExpr(arg) })

            is QueryNode -> {
                when (expr.query.value) {
                    is Get -> {
                        val type: ObjectType = typeAnalysis.type(nameAnalysis.declaration(expr))

                        if (type is VectorType) {
                            readArray(
                                expr.variable.value.name,
                                compilePlaintextExpr(expr.arguments[0])
                            )
                        } else {
                            read(expr.variable.value.name)
                        }
                    }

                    else -> throw Error("backend compilation: unknown query ${expr.query.value.name}")
                }
            }

            // downgrades have no computational content, compile them away
            is DeclassificationNode -> compilePlaintextExpr(expr.expression)

            is EndorsementNode -> compilePlaintextExpr(expr.expression)
        }
    }

    private fun compilePlaintextStmt(
        stmt: StatementNode,
        protocol: Protocol,
        host: Host,
        arrayDecls: MutableList<DeclarationNode>,
        recvCompiler: RecvCompiler,
        sendCompiler: SendCompiler
    ): List<CppStatement> {
        return when (stmt) {
            is LetNode -> {
                val tmp: CppIdentifier = stmt.temporary.value.name

                when (val rhs: ExpressionNode = stmt.value) {
                    is ReceiveNode -> {
                        recvCompiler(stmt.temporary.value, rhs, protocol, host)
                    }

                    is InputNode -> {
                        listOf(
                            input(tmp, cppIntType)
                        )
                    }

                    is PureExpressionNode -> {
                        listOf(
                            declare(tmp, cppIntType, compilePlaintextExpr(rhs))
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
                                elementType = cppIntType,
                                length = compilePlaintextExpr(stmt.arguments[0])
                            )
                        }

                        is ImmutableCellType, is MutableCellType -> {
                            declare(
                                variable = stmt.variable.value.name,
                                type = cppIntType,
                                initVal = compilePlaintextExpr(stmt.arguments[0])
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
                            val index: CppExpression = compilePlaintextExpr(stmt.arguments[0])
                            val rhs: CppExpression = compilePlaintextExpr(stmt.arguments[1])
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
                            val rhs: CppExpression = compilePlaintextExpr(stmt.arguments[0])
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

            is OutputNode ->
                listOf(
                    output(compilePlaintextExpr(stmt.message))
                )

            is SendNode -> sendCompiler(stmt, protocol, host)

            is IfNode ->
                listOf(
                    CppIf(
                        compilePlaintextExpr(stmt.guard),
                        compilePlaintextBlock(stmt.thenBranch, protocol, host, recvCompiler, sendCompiler),
                        compilePlaintextBlock(stmt.elseBranch, protocol, host, recvCompiler, sendCompiler)
                    )
                )

            is InfiniteLoopNode ->
                listOf(
                    loop(compilePlaintextBlock(stmt.body, protocol, host, recvCompiler, sendCompiler))
                )

            is BreakNode -> listOf(loopBreak())

            is AssertionNode -> listOf()

            // this shouldn't be called
            is BlockNode ->
                listOf(
                    compilePlaintextBlock(stmt, protocol, host, recvCompiler, sendCompiler)
                )
        }
    }

    private fun compileLocalReceive(
        tmp: Temporary,
        recv: ReceiveNode,
        @Suppress("UNUSED_PARAMETER") protocol: Protocol,
        host: Host
    ): List<CppStatement> {
        val recvProtocol: Protocol = recv.protocol.value
        return when {
            recvProtocol.hosts.contains(host) -> {
                listOf(
                    receive(tmp.name, cppIntType, recvProtocol, host)
                )
            }

            // allow remote sends from other Local processes
            recvProtocol is Local -> {
                listOf(
                    receive(tmp.name, cppIntType, recvProtocol, recvProtocol.host)
                )
            }

            else -> throw Error("backend compilation: Local process cannot receive from remote non-Local process")
        }
    }

    private fun compileLocalSend(
        send: SendNode,
        @Suppress("UNUSED_PARAMETER") protocol: Protocol,
        host: Host
    ): List<CppStatement> {
        val sendProtocol: Protocol = send.protocol.value
        val cppMessage: CppExpression = compilePlaintextExpr(send.message)

        return when {
            sendProtocol.hosts.contains(host) ->
                listOf(
                    send(sendProtocol, host, cppMessage)
                )

            // only send to remote Local processes
            sendProtocol is Local ->
                listOf(
                    send(sendProtocol, sendProtocol.host, cppMessage)
                )

            else -> throw Error("backend compilation: Local process cannot send to remote non-Local process")
        }
    }

    private fun compileReplicationReceive(
        tmp: Temporary,
        recv: ReceiveNode,
        protocol: Protocol,
        host: Host
    ): List<CppStatement> {
        val recvProtocol: Protocol = recv.protocol.value
        val sendingHosts: Set<Host> = recvProtocol.hosts.intersect(protocol.hosts)
        val receivingHosts: Set<Host> = recvProtocol.hosts.minus(protocol.hosts)

        return when {
            // at least one copy of the Replication process receives
            !sendingHosts.isEmpty() -> {
                when {
                    // copy of Repl process at host receives; broadcast to copies that did not
                    sendingHosts.contains(host) -> {
                        val compiledStmts: MutableList<CppStatement> = mutableListOf()

                        // do actual receive
                        compiledStmts.add(
                            receive(tmp.name, cppIntType, recvProtocol, host)
                        )

                        // broadcast to copies that did not
                        for (receivingHost: Host in receivingHosts) {
                            compiledStmts.add(
                                send(protocol, receivingHost, read(tmp.name))
                            )
                        }

                        compiledStmts
                    }

                    // copy of Repl process at host does not receive; receive from copies that did
                    else -> {
                        val compiledStmts: MutableList<CppStatement> = mutableListOf()
                        val receiveSet: MutableSet<CppIdentifier> = mutableSetOf()
                        val equalPairs: MutableList<Pair<CppIdentifier, CppIdentifier>> = mutableListOf()
                        var i = 1
                        for (sendingHost: Host in sendingHosts) {
                            val receiveName: CppIdentifier = "${tmp.name}__$i"
                            compiledStmts.add(
                                receive(
                                    receiveName,
                                    cppIntType,
                                    protocol,
                                    sendingHost
                                )
                            )

                            receiveSet.add(receiveName)
                            if (i > 1) {
                                equalPairs.add(Pair("${tmp.name}__${i - 1}", receiveName))
                            }

                            i++
                        }

                        // check that all received values are equal
                        val assertExpr: CppExpression =
                            equalPairs.fold(
                                initial = CppTrue as CppExpression,
                                operation = { acc: CppExpression, equalPair: Pair<CppIdentifier, CppIdentifier> ->
                                    val equalExpr: CppExpression =
                                        compileOperator(EqualTo, read(equalPair.first), read(equalPair.second))

                                    compileOperator(And, acc, equalExpr)
                                }
                            )

                        compiledStmts.add(assert(assertExpr))

                        // pick one received value at random and assign it to the real variable
                        compiledStmts.add(
                            declare(tmp.name, cppIntType, read(receiveSet.first()))
                        )

                        compiledStmts
                    }
                }
            }

            // no copy can receive; we cannot compile this
            else -> throw Error("backend compilation: at least one copy of Replication process must be able to receive")
        }
    }

    private fun compileReplicationSend(
        send: SendNode,
        @Suppress("UNUSED_PARAMETER") protocol: Protocol,
        host: Host
    ): List<CppStatement> {
        return when {
            // there is a local copy of the receiving process; send it there
            send.protocol.value.hosts.contains(host) -> {
                listOf(
                    send(
                        send.protocol.value,
                        host,
                        compilePlaintextExpr(send.message)
                    )
                )
            }

            // no local copy of receiving process; don't do anything
            else -> listOf()
        }
    }
}
