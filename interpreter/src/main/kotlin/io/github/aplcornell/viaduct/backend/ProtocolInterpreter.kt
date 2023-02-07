package io.github.aplcornell.viaduct.backend

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap

interface ProtocolInterpreter {
    val availableProtocols: Set<Protocol>

    suspend fun runGuard(protocol: Protocol, expr: AtomicExpressionNode): Value

    suspend fun runSimpleStatement(protocol: Protocol, stmt: SimpleStatementNode)

    suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    )

    suspend fun runReceive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    )

    suspend fun pushContext()

    suspend fun popContext()

    suspend fun pushFunctionContext(arguments: PersistentMap<ParameterNode, Pair<Protocol, FunctionArgumentNode>>)

    suspend fun popFunctionContext()

    fun getContextMarker(): Int

    suspend fun restoreContext(marker: Int)
}

interface ProtocolBackend {
    fun buildProtocolInterpreters(
        host: Host,
        program: ProgramNode,
        protocols: Set<Protocol>,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductRuntime,
        connectionMap: Map<Host, HostAddress>,
    ): Iterable<ProtocolInterpreter>
}
