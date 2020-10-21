package edu.cornell.cs.apl.viaduct

import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider

/** Enumerates the paths of source files under the examples directory. */
internal class ExampleFileProvider(val subdir: String = "") : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        val files = File("examples$subdir").walk().filter { it.isFile }
        return files.map { Arguments.of(it) }.asStream()
    }
}
