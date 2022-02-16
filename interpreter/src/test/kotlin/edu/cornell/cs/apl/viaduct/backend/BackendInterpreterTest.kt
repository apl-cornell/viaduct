package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.mainFunction
import edu.cornell.cs.apl.viaduct.backend.IO.Strategy
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.passes.compile
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
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
        program: edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode,
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

    override suspend fun recvOutput(value: Value) {
    }
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

        val fakeProgram =
            edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode(
                declarations =
                program.hostDeclarations.plus(
                    FunctionDeclarationNode(
                        name = Located(mainFunction, program.sourceLocation),
                        pcLabel = null,
                        parameters = Arguments(program.sourceLocation),
                        body = BlockNode(listOf(), program.sourceLocation),
                        sourceLocation = program.sourceLocation
                    )
                ),
                sourceLocation = program.sourceLocation
            )

        // Run backend interpreter for all hosts.
        runBlocking {
            program.hosts.forEach { host ->
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    backend.run(fakeProgram, host, FakeStrategy)
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
