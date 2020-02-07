package edu.cornell.cs.apl.viaduct

import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream

/** Enumerates the programs under the examples directory. */
internal class ExampleProgramProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return ExampleFileProvider().provideArguments(context)
            .map { Arguments.of(SourceFile.from(it as File).parse()) }
    }
}
