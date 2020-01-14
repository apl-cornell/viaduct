package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * A node in the intermediate language abstract syntax tree.
 *
 * The intermediate language is similar to the surface language, but with the following changes:
 *
 * - For and while loops are elaborated into loop-until-break statements.
 * - Expressions are in A-normal form. Briefly, this means all intermediate results are stored
 *   in temporary variables.
 */
// TODO: what else?
abstract class Node : HasSourceLocation
