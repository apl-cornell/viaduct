package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/** Asserts that [actual] equals [expected], but ignores [SourceLocation]s. */
internal fun assertStructurallyEquals(expected: HasSourceLocation, actual: HasSourceLocation) {
    assertEquals(expected, actual)
}

/**
 * Asserts that [actual] differs from [expected] structurally, that is, a difference in
 * [SourceLocation]s does not count.
 */
internal fun assertStructurallyNotEquals(expected: HasSourceLocation, actual: HasSourceLocation) {
    assertThrows<AssertionFailedError> { assertStructurallyEquals(expected, actual) }
}

private fun assertEquals(expected: Any?, actual: Any?) {
    when {
        expected is SourceLocation && actual is SourceLocation ->
            return

        expected is List<*> && actual is List<*> -> {
            if (expected.size != actual.size)
                fail(expected, actual)
            for (i in expected.indices) {
                assertEquals(expected[i], actual[i])
            }
        }

        expected is HasSourceLocation && actual is HasSourceLocation -> {
            // Actual must be from a compatible class
            if (!actual::class.isInstance(expected))
                fail(expected, actual)

            // Compare all public properties that are backed by a field
            expected::class.memberProperties.forEach {
                if (it.visibility == KVisibility.PUBLIC && it.javaField != null) {
                    assertEquals(it.getter.call(expected), it.getter.call(actual))
                }
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
    if (this is PrettyPrintable) this.toDocument().print() else this.toString()
