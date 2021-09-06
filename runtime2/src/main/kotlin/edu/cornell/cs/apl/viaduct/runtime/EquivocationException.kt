package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host

/** Thrown when a [Host] catches the attacker trying to equivocate. */
class EquivocationException(
    expectedValue: Any?,
    expectedValueProvider: Host,
    actualValue: Any?,
    actualValueProvider: Host
) : ViaductRuntimeException(
    "Equivocation detected: expected $expectedValue ($expectedValueProvider) " +
        "but got $actualValue ($actualValueProvider).")

/** Throws [EquivocationException] if [expectedValue] does not match [actualValue]. */
fun <T> assertEquals(expectedValue: T, expectedValueProvider: Host, actualValue: T, actualValueProvider: Host) {
    if (expectedValue != actualValue)
        throw EquivocationException(expectedValue, expectedValueProvider, actualValue, actualValueProvider)
}
