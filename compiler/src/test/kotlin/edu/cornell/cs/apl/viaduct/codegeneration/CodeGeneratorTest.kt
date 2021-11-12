package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.ProtocolAssignment
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelection
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.Z3Selection
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CodeGeneratorTest {

    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `it generates`(file: File) {
        if (file.parentFile.name != "code-generation") return

        val program = SourceFile.from(file)
            .parse(DefaultCombinedBackend.protocolParsers)
            .elaborated()
            .specialize()

        program.check()

        val protocolFactory = DefaultCombinedBackend.protocolFactory(program)

        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val costEstimator = SimpleCostEstimator(protocolComposer, SimpleCostRegime.LAN)

        val protocolAssignment: ProtocolAssignment =
            ProtocolSelection(
                Z3Selection(),
                protocolFactory,
                protocolComposer,
                costEstimator
            ).selectAssignment(program)

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        /**
         // Post-process program
         val postprocessor = ProgramPostprocessorRegistry(
         ABYMuxPostprocessor(protocolAssignment),
         ZKPMuxPostprocessor(protocolAssignment)
         )
         val postprocessedProgram = postprocessor.postprocess(annotatedProgram)
         **/

        compileKotlinFileSpec(
            annotatedProgram,
            file.name.substringBefore('.'),
            "src",
            listOf<(context: CodeGeneratorContext) -> CodeGenerator>(
                ::PlainTextCodeGenerator,
                ::CommitmentDispatchCodeGenerator
            ),
            DefaultCombinedBackend.protocolComposer
        ).writeTo(System.out)
    }
}
