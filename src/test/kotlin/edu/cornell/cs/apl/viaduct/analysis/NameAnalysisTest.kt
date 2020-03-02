package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TemporaryDefinition
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.Tree
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NameAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it name checks`(program: ProgramNode) {
        NameAnalysis(Tree(program.elaborated())).check()
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `temporary definitions are mapped to reads`(program: ProgramNode) {
        val elaborated = program.elaborated()
        val nameAnalysis = NameAnalysis(Tree(elaborated))
        temporaryDefinitions(elaborated).forEach { definition ->
            nameAnalysis.readers(definition)
        }
    }
}

/** Returns the set of all temporary definitions in the program. */
private fun temporaryDefinitions(program: edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode): Set<TemporaryDefinition> {
    val definitions: MutableSet<TemporaryDefinition> = mutableSetOf()
    fun traverse(node: Node) {
        if (node is TemporaryDefinition) {
            definitions.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(program)
    return definitions
}
