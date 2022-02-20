package edu.cornell.cs.apl.viaduct.examples

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

/** Enumerates [viaductPrograms]. */
class ViaductProgramProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        viaductPrograms.map { Arguments.of(it) }.stream()
}
