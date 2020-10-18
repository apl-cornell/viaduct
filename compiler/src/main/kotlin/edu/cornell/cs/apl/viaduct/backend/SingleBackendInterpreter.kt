package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

class SingleBackendInterpreter(
    private val program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val host: Host,
    private val backends: Map<Protocol, ProtocolInterpreter>
) {
    private val nameAnalysis = NameAnalysis.get(program)

    suspend fun run() {
        val mainBody = program.main.body
        run(nameAnalysis.enclosingFunctionName(mainBody), mainBody)
    }

    suspend fun run(function: FunctionName, stmt: StatementNode) {
        when (stmt) {
            is SimpleStatementNode -> {
                val protocol = protocolAnalysis.primaryProtocol(stmt)
                if (protocol.hosts.contains(this.host)) {
                    backends[protocol]?.runSimpleStatement(stmt)
                        ?: throw ViaductInterpreterError("no backend for protocol ${protocol.asDocument.print()}")
                }

                // TODO: perform synchronization
            }

            is FunctionCallNode -> {
                val argumentProtocolMap: PersistentMap<Protocol, PersistentMap<ParameterNode, FunctionArgumentNode>> =
                    stmt.arguments.fold(persistentMapOf()) { acc, arg ->
                        val parameter = nameAnalysis.parameter(arg)
                        val argProtocol = protocolAnalysis.primaryProtocol(parameter)

                        acc.put(
                            argProtocol,
                            acc[argProtocol]?.put(parameter, arg)
                                ?: persistentMapOf(parameter to arg)
                        )
                    }

                // pass arguments and create new function activation record
                for (kv: Map.Entry<Protocol, ProtocolInterpreter> in backends) {
                    val arguments: PersistentMap<ParameterNode, FunctionArgumentNode> =
                        argumentProtocolMap[kv.key] ?: persistentMapOf()

                    kv.value.pushFunctionContext(arguments)
                }

                // execute function body
                val calledFunction = nameAnalysis.declaration(stmt)
                run(calledFunction.name.value, calledFunction.body)

                // pop function activation record
                for (kv: Map.Entry<Protocol, ProtocolInterpreter> in backends) {
                    kv.value.popFunctionContext()
                }
            }

            is IfNode -> {
                if (protocolAnalysis.hosts(stmt).contains(this.host)) {
                    val guardValue =
                        when (val guard = stmt.guard) {
                            is LiteralNode -> guard.value

                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                backends[guardProtocol]?.runExprAsValue(guard)
                                    ?: throw ViaductInterpreterError("no backend for protocol ${guardProtocol.asDocument.print()}")
                            }
                        }

                    when (guardValue) {
                        is BooleanValue -> {
                            if (guardValue.value) {
                                run(function, stmt.thenBranch)
                            } else {
                                run(function, stmt.elseBranch)
                            }
                        }

                        else -> throw ViaductInterpreterError("conditional guard $guardValue is not boolean")
                    }
                }

                // TODO: perform synchronization
            }

            is InfiniteLoopNode -> {
                if (protocolAnalysis.hosts(stmt).contains(this.host)) {
                    val protocolContextMarkers: Map<Protocol, Int> =
                        backends
                            .map { kv -> kv.key to kv.value.getContextMarker() }
                            .toMap()

                    try {
                        run(function, stmt.body)
                        run(function, stmt)
                    } catch (signal: LoopBreakSignal) {
                        // this signal is for an outer loop
                        if (signal.jumpLabel != stmt.jumpLabel.value) {
                            throw signal
                        } else { // restore context
                            for (protocolContextMarker in protocolContextMarkers) {
                                backends[protocolContextMarker.key]!!.restoreContext(protocolContextMarker.value)
                            }
                        }
                    }
                }

                // TODO: perform synchronization
            }

            is BreakNode -> {
                throw LoopBreakSignal(stmt)
            }

            is BlockNode -> {
                for (backend: ProtocolInterpreter in backends.values) {
                    backend.pushContext()
                }

                for (child: StatementNode in stmt) {
                    run(function, child)
                }

                for (backend: ProtocolInterpreter in backends.values) {
                    backend.popContext()
                }

                // TODO: perform synchronization
            }

            is AssertionNode -> {}
        }
    }
}
