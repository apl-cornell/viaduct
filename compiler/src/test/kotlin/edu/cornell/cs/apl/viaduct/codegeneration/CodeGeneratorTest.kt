package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.validateProtocolAssignment
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CodeGeneratorTest {

    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `it generates`(file: File) {
        if (file.parentFile.name != "plaintext-code-generation") return

        var program = SourceFile.from(file)
            .parse()
            .elaborated()
            .specialize()

        program.check()

        val protocolFactory = simpleProtocolFactory(program)
        val maximizeCost = false

        var wanCost = false
        val protocolComposer = SimpleProtocolComposer
        val costRegime = if (wanCost) SimpleCostRegime.WAN else SimpleCostRegime.LAN
        val costEstimator = SimpleCostEstimator(SimpleProtocolComposer, costRegime)

        val protocolAssignment: (FunctionName, Variable) -> Protocol = selectProtocolsWithZ3(
            program,
            program.main,
            protocolFactory,
            protocolComposer,
            costEstimator,
            if (maximizeCost) CostMode.MAXIMIZE else CostMode.MINIMIZE
        )

        for (processDecl in program.declarations.filterIsInstance<ProcessDeclarationNode>()) {
            validateProtocolAssignment(
                program,
                processDecl,
                protocolFactory,
                protocolComposer,
                costEstimator,
                protocolAssignment
            )
        }

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)

        /**
         // Post-process program
         val postprocessor = ProgramPostprocessorRegistry(
         ABYMuxPostprocessor(protocolAssignment),
         ZKPMuxPostprocessor(protocolAssignment)
         )
         val postprocessedProgram = postprocessor.postprocess(annotatedProgram)
         **/

        val backendCodeGenerator = BackendCodeGenerator(
            annotatedProgram,
            listOf<(context: CodeGeneratorContext) -> CodeGenerator>(::PlainTextCodeGenerator),
            file.name.substringBefore('.'),
            "src"
        )

        backendCodeGenerator.generate()
        println(Document(backendCodeGenerator.generate()).print())
    }
}
