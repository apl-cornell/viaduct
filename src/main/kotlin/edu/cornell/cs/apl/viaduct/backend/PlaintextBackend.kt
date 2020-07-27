package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack

class PlaintextBackend : ProtocolBackend {
    override val supportedProtocols: Set<String> = setOf(Local.protocolName, Replication.protocolName)

    override suspend fun run(
        nameAnalysis: NameAnalysis,
        typeAnalysis: TypeAnalysis,
        runtime: ViaductRuntime,
        program: ProgramNode,
        process: BlockNode,
        projection: ProtocolProjection
    ) {
        val interpreter =
            PlaintextInterpreter(
                nameAnalysis, typeAnalysis,
                ViaductProcessRuntime(runtime, projection),
                program,
                projection
            )

        try {
            interpreter.run(process)
        } catch (signal: LoopBreakSignal) {
            throw ViaductInterpreterError(
                "uncaught loop break signal with jump label ${signal.jumpLabel}", signal.breakNode
            )
        }
    }
}

private class PlaintextInterpreter(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis,
    val runtime: ViaductProcessRuntime,
    val program: ProgramNode,
    val projection: ProtocolProjection
) {
    private val objectStoreStack: Stack<MutableMap<ObjectVariable, PlaintextClassObject>> = Stack()

    private val objectStore: MutableMap<ObjectVariable, PlaintextClassObject>
        get() {
            assert(!objectStoreStack.empty())
            return objectStoreStack.peek()
        }

    private val tempStoreStack: Stack<MutableMap<Temporary, Value>> = Stack()

    private val tempStore: MutableMap<Temporary, Value>
        get() {
            assert(!tempStoreStack.empty())
            return tempStoreStack.peek()
        }

    init {
        objectStoreStack.push(mutableMapOf())
        tempStoreStack.push(mutableMapOf())

        assert(projection.protocol is Local || projection.protocol is Replication)
    }

    private fun pushContext() {
        objectStoreStack.push(objectStore.toMutableMap())
        tempStoreStack.push(tempStore.toMutableMap())
    }

    private fun popContext() {
        objectStoreStack.pop()
        tempStoreStack.pop()
    }

    private suspend fun runExpr(expr: ExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> {
                tempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)
            }

            is QueryNode -> {
                val argValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }

                objectStore[expr.variable.value]?.let { obj ->
                    obj.query(expr.query, argValues)
                } ?: throw UndefinedNameError(expr.variable)
            }

            is OperatorApplicationNode -> {
                val operandValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }
                expr.operator.apply(operandValues)
            }

            is DowngradeNode -> runExpr(expr.expression)

            is InputNode -> {
                when (projection.protocol) {
                    is Local -> runtime.input()

                    else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", expr)
                }
            }

            is ReceiveNode -> {
                val sendProtocol = expr.protocol.value

                when (projection.protocol) {
                    is Local -> {
                        return when {
                            // receive from local process
                            sendProtocol.hosts.contains(projection.host) -> {
                                runtime.receive(ProtocolProjection(sendProtocol, projection.host))
                            }

                            // allow remote sends from other Local processes
                            sendProtocol is Local -> {
                                runtime.receive(ProtocolProjection(sendProtocol, sendProtocol.host))
                            }

                            else -> {
                                throw ViaductInterpreterError(
                                    "cannot receive from protocol ${sendProtocol.name} to $projection"
                                )
                            }
                        }
                    }

                    is Replication -> {
                        val sendingHosts: Set<Host> = sendProtocol.hosts.intersect(projection.protocol.hosts)
                        val receivingHosts: Set<Host> = sendProtocol.hosts.minus(projection.protocol.hosts)

                        return if (!sendingHosts.isEmpty()) { // at least one copy of the Replication process receives
                            // do actual receive
                            val receivedValue: Value =
                                runtime.receive(ProtocolProjection(sendProtocol, projection.host))

                            // broadcast to projections that did not receive
                            for (receivingHost: Host in receivingHosts) {
                                runtime.send(receivedValue, ProtocolProjection(projection.protocol, receivingHost))
                            }

                            receivedValue
                        } else { // copy of Repl process at host does not receive; receive from copies that did
                            var finalValue: Value? = null
                            for (sendingHost: Host in sendingHosts) {
                                val receivedValue: Value =
                                    runtime.receive(ProtocolProjection(projection.protocol, sendingHost))

                                if (finalValue == null) {
                                    finalValue = receivedValue
                                } else if (finalValue != receivedValue) {
                                    throw ViaductInterpreterError("received different values")
                                }
                            }

                            finalValue ?: throw ViaductInterpreterError("did not receive")
                        }
                    }

                    else -> throw ViaductInterpreterError(
                        "cannot receive from protocol $sendProtocol from $projection"
                    )
                }
            }
        }
    }

    private fun runDeclaration(
        objectName: ObjectVariableNode,
        className: ClassNameNode,
        objectType: ObjectType,
        arguments: List<Value>
    ) {
        when (objectType) {
            is ImmutableCellType -> {
                objectStore[objectName.value] =
                    ImmutableCellObject(arguments[0], objectName, objectType)
            }

            is MutableCellType -> {
                objectStore[objectName.value] =
                    MutableCellObject(arguments[0], objectName, objectType)
            }

            is VectorType -> {
                val length = arguments[0] as IntegerValue
                objectStore[objectName.value] =
                    VectorObject(length.value, objectType.elementType.defaultValue, objectName, objectType)
            }

            else -> throw UndefinedNameError(className)
        }
    }

    suspend fun run(stmt: StatementNode) {
        when (stmt) {
            is DeclarationNode -> {
                val objectType: ObjectType = typeAnalysis.type(stmt)
                val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }
                runDeclaration(stmt.variable, stmt.className, objectType, argValues)
            }

            is LetNode -> {
                val rhsValue: Value = runExpr(stmt.value)
                tempStore[stmt.temporary.value] = rhsValue
            }

            is UpdateNode -> {
                val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }

                objectStore[stmt.variable.value]
                    ?.update(stmt.update, argValues)
                    ?: throw UndefinedNameError(stmt.variable)
            }

            is SendNode -> {
                val msgValue: Value = runExpr(stmt.message)
                val recvProtocol: Protocol = stmt.protocol.value

                when (projection.protocol) {
                    is Local -> {
                        if (recvProtocol.hosts.contains(projection.host)) {
                            runtime.send(msgValue, ProtocolProjection(stmt.protocol.value, projection.host))
                        } else if (recvProtocol is Local) { // only send to remote Local processes
                            for (recvHost: Host in stmt.protocol.value.hosts) {
                                runtime.send(msgValue, ProtocolProjection(stmt.protocol.value, recvHost))
                            }

                            runtime.send(msgValue, ProtocolProjection(recvProtocol, recvProtocol.host))
                        }
                    }

                    is Replication -> {
                        // there is a local copy of the receiving process; send it there
                        // otherwise, don't do anything
                        if (recvProtocol.hosts.contains(projection.host)) {
                            runtime.send(msgValue, ProtocolProjection(recvProtocol, projection.host))
                        }
                    }
                }
            }

            is OutputNode -> {
                when (projection.protocol) {
                    is Local -> {
                        val outputVal: Value = runExpr(stmt.message)
                        runtime.output(outputVal)
                    }

                    else -> throw Exception("cannot perform I/O in non-Local protocol")
                }
            }

            is IfNode -> {
                val guardVal = runExpr(stmt.guard) as BooleanValue

                if (guardVal.value) {
                    run(stmt.thenBranch)
                } else {
                    run(stmt.elseBranch)
                }
            }

            is InfiniteLoopNode -> {
                // communicate loop break by exception
                try {
                    run(stmt.body)
                    run(stmt)
                } catch (signal: LoopBreakSignal) { // catch loop break signal
                    // this signal is for an outer loop
                    if (signal.jumpLabel != null && signal.jumpLabel != stmt.jumpLabel.value) {
                        throw signal
                    }
                }
            }

            is BreakNode -> throw LoopBreakSignal(stmt)

            is BlockNode -> {
                pushContext()

                for (childStmt: StatementNode in stmt) {
                    run(childStmt)
                }

                popContext()
            }
        }
    }
}
