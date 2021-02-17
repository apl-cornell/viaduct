package edu.cornell.cs.apl.viaduct

import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider

/** Enumerates the paths of source files that should successfully compile. */
class PositiveTestFileProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-pass").map { Arguments.of(it) }.asStream()
}

/** Enumerates the paths of source files that should fail to compile. */
class NegativeTestFileProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-fail").map { Arguments.of(it) }.asStream()
}

private fun testFilesAtPath(path: String): Sequence<File> {
    // TODO: sorting will break with subdirectories
    return File("tests").resolve(path).walk().filter { it.isFile }.sorted()
}
