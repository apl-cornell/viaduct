package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.Host

/** Thrown when a [Host] catches the attacker trying to equivocate. */
class EquivocationException private constructor(
    expectedValue: Any?,
    expectedValueProvider: Host,
    actualValue: Any?,
    actualValueProvider: Host
) : ViaductRuntimeException(
    "Equivocation detected: expected $expectedValue ($expectedValueProvider) " +
        "but got $actualValue ($actualValueProvider)."
) {
    companion object {
        /** Throws [EquivocationException] if [expectedValue] does not match [actualValue]. */
        fun <T> assertEquals(expectedValue: T, expectedValueProvider: Host, actualValue: T, actualValueProvider: Host) {
            if (expectedValue != actualValue)
                throw EquivocationException(expectedValue, expectedValueProvider, actualValue, actualValueProvider)
        }

        /** Throws [EquivocationException] if [expectedValue] does not match [actualValue]. */
        fun <T> assertEquals(
            expectedValue: Array<T>,
            expectedValueProvider: Host,
            actualValue: Array<T>,
            actualValueProvider: Host
        ) {
            if (!expectedValue.contentDeepEquals(actualValue))
                throw EquivocationException(expectedValue, expectedValueProvider, actualValue, actualValueProvider)
        }
    }
}
