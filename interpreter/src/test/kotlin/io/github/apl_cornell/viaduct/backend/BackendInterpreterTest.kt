package io.github.apl_cornell.viaduct.backend

import io.github.apl_cornell.viaduct.PositiveTestFileProvider
import io.github.apl_cornell.viaduct.analysis.ProtocolAnalysis
import io.github.apl_cornell.viaduct.backend.IO.Strategy
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.SourceFile
import io.github.apl_cornell.viaduct.passes.compile
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.selection.SimpleCostRegime
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.apl_cornell.viaduct.syntax.values.BooleanValue
import io.github.apl_cornell.viaduct.syntax.values.IntegerValue
import io.github.apl_cornell.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors

private class FakeProtocolInterpreter(
    override val availableProtocols: Set<Protocol>
) : ProtocolInterpreter {
    override suspend fun runGuard(protocol: Protocol, expr: AtomicExpressionNode): Value {
        return BooleanValue(false)
    }

    override suspend fun runSimpleStatement(protocol: Protocol, stmt: SimpleStatementNode) {}

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ) {
    }

    override suspend fun runReceive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ) {
    }

    override suspend fun pushContext() {}

    override suspend fun popContext() {}

    override suspend fun pushFunctionContext(arguments: PersistentMap<ParameterNode, Pair<Protocol, FunctionArgumentNode>>) {}

    override suspend fun popFunctionContext() {}

    override fun getContextMarker(): Int = 0

    override suspend fun restoreContext(marker: Int) {}
}

/** Fake protocol backend that doesn't do anything. */
private object FakeProtocolBackend : ProtocolBackend {
    override fun buildProtocolInterpreters(
        host: Host,
        program: io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode,
        protocols: Set<Protocol>,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductRuntime,
        connectionMap: Map<Host, HostAddress>
    ): Iterable<ProtocolInterpreter> {
        return setOf(FakeProtocolInterpreter(protocols))
    }
}

private object FakeStrategy : Strategy {
    override suspend fun getInput(): Value {
        return IntegerValue(0)
    }

    override suspend fun recvOutput(value: Value) {}
}

private fun findAvailableTcpPort() =
    ServerSocket(0).use { it.localPort }

internal class BackendInterpreterTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun testInterpreter(file: File) {
        val program = SourceFile.from(file).compile(DefaultCombinedBackend, costRegime = SimpleCostRegime.LAN)

        val hostAddresses = program.hosts.associateWith {
            HostAddress(InetAddress.getLoopbackAddress().hostAddress, findAvailableTcpPort())
        }

        val backend = ViaductBackend(listOf(FakeProtocolBackend), hostAddresses)

        // Run backend interpreter for all hosts.
        runBlocking {
            program.hosts.forEach { host ->
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    backend.run(program, host, FakeStrategy)
                }
            }
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setLogLevel() {
            Configurator.setRootLevel(Level.INFO)
        }
    }
}
