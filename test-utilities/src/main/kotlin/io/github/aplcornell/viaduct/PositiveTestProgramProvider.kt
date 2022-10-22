package io.github.aplcornell.viaduct

import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.parsing.SourceFile
import io.github.aplcornell.viaduct.parsing.parse
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream

/** Same as [PositiveTestFileProvider] but parses the programs. */
class PositiveTestProgramProvider(private val subfolder: String = "") : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return PositiveTestFileProvider(subfolder).provideArguments(context)
            .map { Arguments.of(SourceFile.from(it.get()[0] as File).parse(DefaultCombinedBackend.protocolParsers)) }
    }
}
