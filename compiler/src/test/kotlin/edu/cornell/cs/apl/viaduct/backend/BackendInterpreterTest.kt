package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.passes.Splitter
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/** Fake protocol backend that doesn't do anything. */
private object FakeBackend : ProtocolBackend {
    override suspend fun run(
        runtime: ViaductProcessRuntime,
        program: edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode,
        process: BlockNode
    ) {
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
        val protocolAssignment =
            selectProtocolsWithZ3(program, program.main, simpleProtocolFactory(program), ::simpleProtocolCost)
        val protocolAnalysis = ProtocolAnalysis(program, protocolAssignment)

        // Split the program.
        val splitProgram = Splitter(protocolAnalysis).splitMain()

        // set up backend interpreter with fake backends
        val backendMap: Map<ProtocolName, ProtocolBackend> =
            splitProgram.declarations
                .filterIsInstance<ProcessDeclarationNode>()
                .filter { procDecl -> procDecl.protocol.value !is HostInterface }
                .map { procDecl -> Pair(procDecl.protocol.value.protocolName, FakeBackend) }
                .toMap()

        val hosts: Set<Host> =
            splitProgram.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl -> hostDecl.name.value }
                .toSet()

        val interpreter = BackendInterpreter(backendMap)

        // run backend interpreter for all hosts
        runBlocking {
            for (host: Host in hosts) {
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    interpreter.run(splitProgram, host)
                }
            }
        }
    }
}
