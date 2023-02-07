package io.github.aplcornell.viaduct.examples

import io.github.aplcornell.viaduct.runtime.ViaductGeneratedProgram
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

/** Enumerates [viaductPrograms] that do not use ABY. */
class ViaductProgramProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        viaductPrograms.filter { !usesABY(it) }.map { Arguments.of(it) }.stream()
}

/** Enumerates [viaductPrograms] that use ABY. */
class ViaductABYProgramProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        viaductPrograms.filter { usesABY(it) }.map { Arguments.of(it) }.stream()
}

private fun usesABY(program: ViaductGeneratedProgram): Boolean =
    program::class.qualifiedName!!.contains("aby")
