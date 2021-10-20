package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backend.IO.Strategy
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.core.config.Configurator
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
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

internal class BackendInterpreterTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun testInterpreter(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated().specialize()

        // Perform static checks.
        program.check()

        // Select protocols.
        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val protocolAssignment: (FunctionName, Variable) -> Protocol =
            selectProtocolsWithZ3(
                program,
                program.main,
                simpleProtocolFactory(program),
                protocolComposer,
                SimpleCostEstimator(protocolComposer, SimpleCostRegime.LAN),
                CostMode.MINIMIZE
            )
        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        // set up backend interpreter with fake backends
        val hosts: Set<Host> =
            program.hostDeclarations
                .map { hostDecl -> hostDecl.name.value }
                .toSet()

        val backend = ViaductBackend(listOf(FakeProtocolBackend))

        val fakeProgram =
            edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode(
                declarations =
                annotatedProgram.hostDeclarations.plus(
                    ProcessDeclarationNode(
                        protocol = Located(MainProtocol, annotatedProgram.sourceLocation),
                        body = BlockNode(listOf(), annotatedProgram.sourceLocation),
                        sourceLocation = annotatedProgram.sourceLocation
                    )
                ),
                sourceLocation = annotatedProgram.sourceLocation
            )

        Configurator.setRootLevel(org.apache.logging.log4j.Level.INFO)

        // run backend interpreter for all hosts
        runBlocking {
            for (host: Host in hosts) {
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    backend.run(fakeProgram, host, FakeStrategy)
                }
            }
        }
    }
}
