package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

private object FakeProtocolInterpreter : ProtocolInterpreter {
    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value =
        BooleanValue(false)

    override suspend fun runSimpleStatement(stmt: SimpleStatementNode) {}

    override suspend fun pushContext() {}

    override suspend fun popContext() {}

    override suspend fun pushFunctionContext(arguments: PersistentMap<ParameterNode, FunctionArgumentNode>) {}

    override suspend fun popFunctionContext() {}

    override fun getContextMarker(): Int = 0

    override suspend fun restoreContext(marker: Int) {}
}

/** Fake protocol backend that doesn't do anything. */
private object FakeProtocolInterpreterFactory : ProtocolInterpreterFactory {
    override fun buildProtocolInterpreter(
        program: edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductProcessRuntime,
        connectionMap: Map<Host, HostAddress>
    ): ProtocolInterpreter {
        return FakeProtocolInterpreter
    }
}

internal class BackendInterpreterTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun testInterpreter(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated().specialize()

        // Perform static checks.
        program.check()

        // Select protocols.
        val protocolAssignment: (FunctionName, Variable) -> Protocol =
            selectProtocolsWithZ3(program, program.main, SimpleProtocolFactory(program), SimpleCostEstimator)
        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)
        val protocolAnalysis = ProtocolAnalysis(annotatedProgram, SimpleProtocolComposer)

        // set up backend interpreter with fake backends
        val backendMap: Map<ProtocolName, ProtocolInterpreterFactory> =
            protocolAnalysis.participatingProtocols(annotatedProgram)
                .map { protocol -> Pair(protocol.protocolName, FakeProtocolInterpreterFactory) }
                .toMap()

        ViaductBackend(backendMap)

        /*
        val hosts: Set<Host> =
            program.hosts
                .map { hostDecl -> hostDecl.name.value }
                .toSet()

        val interpreter = ViaductBackend(backendMap)

        // run backend interpreter for all hosts
        runBlocking {
            for (host: Host in hosts) {
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    interpreter.run(annotatedProgram, host)
                }
            }
        }
        */
    }
}
