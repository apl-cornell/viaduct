package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class InformationFlowAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it information flow checks`(program: ProgramNode) {
        val nameAnalysis = NameAnalysis(Tree(program.elaborated()))
        InformationFlowAnalysis(nameAnalysis).check()
    }
}
