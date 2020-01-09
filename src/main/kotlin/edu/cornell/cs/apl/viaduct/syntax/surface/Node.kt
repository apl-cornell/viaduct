package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

/** A node in the abstract syntax tree of a surface level program. */
abstract class Node {
    abstract val sourceLocation: SourceLocation
}
