package edu.cornell.cs.apl.viaduct.imp.ast2

import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange

/** Maintains a source location. */
abstract class Located {
    var sourceLocation: SourceRange? = null
}
