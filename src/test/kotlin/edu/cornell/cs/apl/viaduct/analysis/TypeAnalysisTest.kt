package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.Tree
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class TypeAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it type checks`(program: ProgramNode) {
        val nameAnalysis = NameAnalysis(Tree(program.elaborated()))
        TypeAnalysis(nameAnalysis).check()
    }
}
