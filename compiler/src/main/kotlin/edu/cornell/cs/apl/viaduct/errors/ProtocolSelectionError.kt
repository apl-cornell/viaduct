package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Variable

/**
 * Thrown when an error occurs during protocol selection.
 */
abstract class ProtocolSelectionError : CompilationError() {
    override val category: String
        get() = "Protocol Selection"
}

/**
 * Thrown when the protocol selection cannot find a solution.
 */
class NoSelectionSolutionError : ProtocolSelectionError() {
    override val source: String
        get() = ""

    override val description: Document
        get() = Document("Could not find a protocol selection for this program.\n")
}

/**
 * Thrown when the protocol selection does not have a solution for a program variable.
 *
 * @param f The name of the function enclosing the variable.
 * @param v The name of variable.
 */
class NoVariableSelectionSolutionError(
    private val f: FunctionName,
    private val v: Variable
) : ProtocolSelectionError() {
    override val source: String
        get() = ""

    override val description: Document
        get() = Document("No protocol selection found for variable $v at function $f.\n")
}

/**
 * Thrown when a protocol index is not mapped to a corresponding protocol.
 *
 * @param protocolIndex The protocol index without a mapping to an actual protocol.
 */
class NoProtocolIndexMapping(
    private val protocolIndex: Int
) : ProtocolSelectionError() {
    override val source: String
        get() = ""

    override val description: Document
        get() = Document("No mapping found for protocol index $protocolIndex\n")
}
