package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Synchronization
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger("Interpreter")

class BackendInterpreter(
    private val host: Host,
    private val program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val protocolInterpreters: List<ProtocolInterpreter>,
    private val runtime: ViaductProcessRuntime
) {
    private val nameAnalysis = NameAnalysis.get(program)
    private val protocolInterpreterMap: Map<Protocol, ProtocolInterpreter>
    private val syncProtocol = Synchronization(program.hostDeclarations.map { it.name.value }.toSet())

    init {
        val initInterpreterMap: MutableMap<Protocol, ProtocolInterpreter> = mutableMapOf()
        val currentProtocols: MutableSet<Protocol> = mutableSetOf()
        for (interpreter in protocolInterpreters) {
            val interpreterProtocols = interpreter.availableProtocols
            for (protocol in interpreterProtocols) {
                if (currentProtocols.contains(protocol)) {
                    throw ViaductInterpreterError("Interpreter: Multiple backends for protocol ${protocol.asDocument.print()}")
                } else {
                    currentProtocols.add(protocol)
                    initInterpreterMap[protocol] = interpreter
                }
            }
        }

        val hostParticipatingProtocols =
            protocolAnalysis
                .participatingProtocols(program)
                .filter { protocol -> protocol.hosts.contains(host) }

        for (protocol in hostParticipatingProtocols) {
            if (!currentProtocols.contains(protocol)) {
                throw ViaductInterpreterError("Interpreter: No backend for ${protocol.protocolName}")
            }
        }

        protocolInterpreterMap = initInterpreterMap
    }

    suspend fun run() {
        val mainBody = program.main.body

        logger.info { "starting interpretation" }

        val duration = measureTimeMillis {
            run(nameAnalysis.enclosingFunctionName(mainBody), mainBody)
            // synchronize(allHosts, allHosts)
        }

        logger.info { "finished interpretation, total running time: ${duration}ms" }
    }

    /** Synchronize hosts. */
    suspend fun synchronize(senders: Set<Host>, receivers: Set<Host>) {
        if (receivers.isNotEmpty()) {
            logger.info {
                "synchronizing hosts ${senders.joinToString(", ") { it.name }} " +
                    "with ${receivers.joinToString(", ") { it.name }}"
            }
        }

        if (senders.contains(this.host)) {
            for (receiver in receivers) {
                if (this.host != receiver) {
                    runtime.send(UnitValue, ProtocolProjection(syncProtocol, receiver))
                }
            }
        }

        if (receivers.contains(this.host)) {
            for (sender in senders) {
                if (this.host != sender) {
                    runtime.receive(ProtocolProjection(syncProtocol, sender))
                }
            }
        }
    }

    suspend fun run(function: FunctionName, stmt: StatementNode) {
        when (stmt) {
            is LetNode -> {
                val protocol = protocolAnalysis.primaryProtocol(stmt)
                var reader: SimpleStatementNode? = null
                var readerProtocol: Protocol? = null
                var events: ProtocolCommunication? = null

                // there should only be a single reader, if any
                val readers = nameAnalysis.readers(stmt).filterIsInstance<SimpleStatementNode>()
                if (readers.isNotEmpty()) {
                    reader = readers.first()
                    readerProtocol = protocolAnalysis.primaryProtocol(reader)
                    events = protocolAnalysis.relevantCommunicationEvents(stmt, reader)
                }

                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
                    // execute statement, if host is participating
                    val protocolBackend = protocolInterpreterMap[protocol]
                        ?: throw ViaductInterpreterError("no backend for protocol ${protocol.asDocument.print()}")
                    protocolBackend.runSimpleStatement(protocol, stmt)

                    // send data
                    if (readers.isNotEmpty()) {
                        protocolBackend.runSend(stmt, protocol, reader!!, readerProtocol!!, events!!)
                    }
                }

                // receive data
                if (readers.isNotEmpty()) {
                    if (protocolAnalysis.participatingHosts(reader!!).contains(this.host)) {
                        protocolInterpreterMap[readerProtocol]
                            ?.runReceive(stmt, protocol, reader, readerProtocol!!, events!!)
                            ?: throw ViaductInterpreterError(
                                "no backend for protocol ${readerProtocol!!.asDocument.print()}"
                            )
                    }
                }

                // synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    protocolInterpreterMap[protocol]?.runSimpleStatement(protocol, stmt)
                        ?: throw ViaductInterpreterError("no backend for protocol ${protocol.asDocument.print()}")
                }

                // synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is FunctionCallNode -> {
                val argumentProtocolMap: Map<ParameterNode, Pair<Protocol, FunctionArgumentNode>> =
                    stmt.arguments.map { arg ->
                        val parameter = nameAnalysis.parameter(arg)
                        val argProtocol = protocolAnalysis.primaryProtocol(parameter)
                        parameter to (argProtocol to arg)
                    }.toMap()

                // pass arguments and create new function activation record
                for (interpreter in protocolInterpreters) {
                    val arguments: PersistentMap<ParameterNode, Pair<Protocol, FunctionArgumentNode>> =
                        argumentProtocolMap
                            .filter { kv -> interpreter.availableProtocols.contains(kv.value.first) }
                            .toPersistentMap()

                    interpreter.pushFunctionContext(arguments)
                }

                // execute function body
                val calledFunction = nameAnalysis.declaration(stmt)
                run(calledFunction.name.value, calledFunction.body)

                // pop function activation record
                for (interpreter in protocolInterpreters) {
                    interpreter.popFunctionContext()
                }
            }

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
                    val guardValue =
                        when (val guard = stmt.guard) {
                            is LiteralNode -> guard.value

                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                protocolInterpreterMap[guardProtocol]?.runGuard(guardProtocol, guard)
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

                // synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is InfiniteLoopNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(this.host)) {
                    val contextMarkers: Map<ProtocolInterpreter, Int> =
                        protocolInterpreters
                            .map { interpreter -> interpreter to interpreter.getContextMarker() }
                            .toMap()

                    try {
                        /*
                        run(function, stmt.body)
                        run(function, stmt)
                         */

                        while (true) {
                            run(function, stmt.body)
                        }
                    } catch (signal: LoopBreakSignal) {
                        // this signal is for an outer loop
                        if (signal.jumpLabel != stmt.jumpLabel.value) {
                            throw signal
                        } else { // restore context
                            for (contextMarker in contextMarkers) {
                                contextMarker.key.restoreContext(contextMarker.value)
                            }
                        }
                    }
                }

                // synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is BreakNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    throw LoopBreakSignal(stmt)
                }
            }

            is BlockNode -> {
                for (interpreter in protocolInterpreters) {
                    interpreter.pushContext()
                }

                for (child: StatementNode in stmt) {
                    run(function, child)
                }

                for (interpreter in protocolInterpreters) {
                    interpreter.popContext()
                }

                // synchronize(protocolAnalysis.participatingHosts(stmt), protocolAnalysis.hostsToSync(stmt))
            }

            is AssertionNode -> {
            }
        }
    }
}
