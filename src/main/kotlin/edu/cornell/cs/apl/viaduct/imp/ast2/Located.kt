package edu.cornell.cs.apl.viaduct.imp.ast2

import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange

/** Maintains a source location. */
abstract class Located(private val sourceLocationMetadata: Metadata<SourceRange> = Metadata()) {
    var sourceLocation: SourceRange?
        get() = sourceLocationMetadata.data
        set(value) {
            sourceLocationMetadata.data = value
        }
}
