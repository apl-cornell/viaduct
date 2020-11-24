package edu.cornell.cs.apl.viaduct

import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import java.io.File
import java.util.stream.Stream
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider

/** Same as [PositiveTestFileProvider] but parses the programs. */
internal class PositiveTestProgramProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return PositiveTestFileProvider().provideArguments(context)
            .map { Arguments.of(SourceFile.from(it.get()[0] as File).parse()) }
    }
}
