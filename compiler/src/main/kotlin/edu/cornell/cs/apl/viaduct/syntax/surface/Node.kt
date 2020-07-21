package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * A node in the abstract syntax tree of a surface level program.
 *
 * The topmost level node is [ProgramNode].
 */
abstract class Node : HasSourceLocation, PrettyPrintable
