package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

/** Asserts that [actual] equals [expected], but ignores [SourceLocation]s. */
// TODO: extend this to also work with intermediate Nodes.
internal fun assertStructurallyEquals(expected: Node, actual: Node) {
    // Actual must be from a compatible class
    if (!expected::class.isInstance(actual))
        fail(expected, actual)

    // Compare all public properties that are backed by a field
    expected::class.memberProperties.forEach {
        if (it.visibility == KVisibility.PUBLIC && it.javaField != null) {
            assertEquals(it.getter.call(expected), it.getter.call(actual))
        }
    }
}

/**
 * Asserts that [actual] differs from [expected] structurally, that is, a difference in
 * [SourceLocation]s does not count.
 */
internal fun assertStructurallyNotEquals(expected: Node, actual: Node) {
    assertThrows<AssertionFailedError> { assertStructurallyEquals(expected, actual) }
}

private fun assertEquals(expected: Any?, actual: Any?) {
    when {
        expected is Node && actual is Node ->
            assertStructurallyEquals(expected, actual)

        expected is Located<*> && actual is Located<*> ->
            assertEquals(expected.value, actual.value)

        expected is SourceLocation && actual is SourceLocation ->
            return

        expected is List<*> && actual is List<*> -> {
            if (expected.size != actual.size)
                fail(expected, actual)
            for (i in expected.indices) {
                assertEquals(expected[i], actual[i])
            }
        }

        else ->
            if (expected != actual)
                fail(expected, actual)
    }
}

private fun fail(expected: Any?, actual: Any?) {
    org.junit.jupiter.api.fail {
        "Actual AST does not match the expected one." +
            "\nExpected: ${expected.print()}" +
            "\nActual: ${actual.print()}"
    }
}

private fun Any?.print(): String =
    if (this is PrettyPrintable) this.asDocument.print() else this.toString()
