package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.splitMain
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.selection.SimpleFactory
import edu.cornell.cs.apl.viaduct.selection.SimpleSelection
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.Variable
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
    override suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode) {}
}

internal class BackendInterpreterTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun testInterpreter(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        val nameAnalysis = NameAnalysis(Tree(program))
        val typeAnalysis = TypeAnalysis(nameAnalysis)
        val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

        // Perform static checks.
        nameAnalysis.check()
        typeAnalysis.check()
        informationFlowAnalysis.check()

        // Select protocols.
        val protocolAssignment: (Variable) -> Protocol =
            SimpleSelection(
                SimpleFactory(
                    nameAnalysis,
                    informationFlowAnalysis
                ), ::simpleProtocolCost
            )
                .select(program.main, nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, protocolAssignment)

        // Split the program.
        val splitProgram = program.splitMain(protocolAnalysis, typeAnalysis)

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
