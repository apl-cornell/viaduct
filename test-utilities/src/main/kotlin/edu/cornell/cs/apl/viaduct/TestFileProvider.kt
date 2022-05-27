package edu.cornell.cs.apl.viaduct

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

/** Enumerates the paths of source files that should successfully compile. */
class PositiveTestFileProvider(private val subfolder: String = "") : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-pass/$subfolder").map { Arguments.of(it) }.asStream()
}

/** Enumerates the paths of source files that should fail to compile. */
class NegativeTestFileProvider(private val subfolder: String = "") : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-fail/$subfolder").map { Arguments.of(it) }.asStream()
}

private fun testFilesAtPath(path: String): Sequence<File> {
    // TODO: sorting will break with subdirectories
    return File("tests").resolve(path).walk()
        .filter { it.isFile }
        .filter { it.extension == "via" }
        .sorted()
}
