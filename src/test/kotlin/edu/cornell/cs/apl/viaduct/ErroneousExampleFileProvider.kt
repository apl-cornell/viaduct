package edu.cornell.cs.apl.viaduct

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Enumerates the paths of source files under the errors directory.
 * These are programs that contain errors.
 */
internal class ErroneousExampleFileProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        val files = File("errors").walk().filter { it.isFile }
        return files.map { Arguments.of(it) }.asStream()
    }
}
