package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Synchronization
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
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BackendInterpreter(
    private val program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val host: Host,
    inputBackends: Map<Protocol, ProtocolInterpreter>,
    private val runtime: ViaductRuntime
) {
    private val allHosts = program.hosts.map { it.name.value }.toSet()
    private val nameAnalysis = NameAnalysis.get(program)
    private val backends: Map<Protocol, ProtocolInterpreter> =
        inputBackends.filter { kv -> kv.key !is Synchronization }
    private val syncProtocol = Synchronization(program.hosts.map { it.name.value }.toSet())

    suspend fun run() {
        val mainBody = program.main.body

        logger.info { "starting interpretation" }

        run(nameAnalysis.enclosingFunctionName(mainBody), mainBody)
        synchronize(allHosts, allHosts)

        logger.info { "finished interpretation" }
    }

    /** Synchronize hosts. */
    suspend fun synchronize(senders: Set<Host>, receivers: Set<Host>) {
        if (receivers.isNotEmpty()) {
            logger.info {
                "synchronizing hosts ${senders.joinToString(", "){ it.name } } " +
                    "with ${receivers.joinToString(", ") { it.name }}"
            }
        }

        if (senders.contains(this.host)) {
            for (receiver in receivers) {
                if (this.host != receiver) {
                    runtime.send(
                        UnitValue,
                        ProtocolProjection(syncProtocol, this@BackendInterpreter.host),
                        ProtocolProjection(syncProtocol, receiver)
                    )
                }
            }
        }

        if (receivers.contains(this.host)) {
            for (sender in senders) {
                if (this.host != sender) {
                    runtime.receive(
                        ProtocolProjection(syncProtocol, sender),
                        ProtocolProjection(syncProtocol, this@BackendInterpreter.host)
                    )
                }
            }
        }
    }

    suspend fun run(function: FunctionName, stmt: StatementNode) {
        when (stmt) {
            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    backends[protocol]?.runSimpleStatement(stmt)
                        ?: throw ViaductInterpreterError("no backend for protocol ${protocol.asDocument.print()}")
                }

                synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
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
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
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

                synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is InfiniteLoopNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
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

                synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
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

                synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is AssertionNode -> {
            }
        }
    }
}
