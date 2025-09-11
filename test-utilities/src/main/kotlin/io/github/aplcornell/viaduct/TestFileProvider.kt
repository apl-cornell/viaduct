package io.github.aplcornell.viaduct

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

/** Enumerates the paths of source files that should successfully compile. */
class PositiveTestFileProvider(private val subfolder: String = "", private val extension: String = "via") :
    ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-pass/$subfolder", extension).map { Arguments.of(it) }.asStream()
}

/** Enumerates the paths of source files that should fail to compile. */
class NegativeTestFileProvider(private val subfolder: String = "", private val extension: String = "via") :
    ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
        testFilesAtPath("should-fail/$subfolder", extension).map { Arguments.of(it) }.asStream()
}

/** Enumerates the paths of source files that use the circuit representation and should successfully compile. */
class CircuitTestFileProvider(subfolder: String = "") :
    ArgumentsProvider by PositiveTestFileProvider(subfolder, "circuit")

private fun testFilesAtPath(
    path: String,
    extension: String,
): Sequence<File> {
    // TODO: sorting will break with subdirectories
    return File("tests").resolve(path).walk()
        .filter { it.isFile }
        .filter { it.extension == extension }
        .sorted()
}
