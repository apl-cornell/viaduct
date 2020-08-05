package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.surface.assertStructurallyEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NodeTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `copy returns structurally equivalent nodes`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        assertStructurallyEquals(program.toSurfaceNode(), program.deepCopy().toSurfaceNode())
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `copy uses all children nodes`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        program.trackingDeepCopy()
    }
}

private fun Node.deepCopy(): Node =
    this.copy(this.children.toList().map { it.deepCopy() })

private fun Node.trackingDeepCopy(): Node {
    val copiedChildren = TrackingList(this.children.toList().map { it.trackingDeepCopy() })
    val copied = this.copy(copiedChildren)
    assertEquals(0.until(copiedChildren.size).toSet(), copiedChildren.accessedIndices)
    return copied
}

/** A list that tracks which indices have been accesses. */
private class TrackingList<out A>(private val list: List<A>) : List<A> by list {
    val accessedIndices = mutableSetOf<Int>()

    override fun get(index: Int): A {
        accessedIndices.add(index)
        return list[index]
    }

    override fun iterator(): Iterator<A> = TrackingIterator()

    inner class TrackingIterator : Iterator<A> {
        private var index = 0

        override fun hasNext(): Boolean = index < list.size

        override fun next(): A {
            val result = this@TrackingList[index]
            index += 1
            return result
        }
    }
}
