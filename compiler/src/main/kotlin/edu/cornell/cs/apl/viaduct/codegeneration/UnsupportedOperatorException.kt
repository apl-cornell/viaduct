package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node

/**
 * Thrown when a protocol is assigned an operator it cannot implement.
 * Usually indicates a bug in protocol selection.
 */
class UnsupportedOperatorException(protocol: Protocol, node: Node) :
    CodeGenerationException("Protocol ${protocol.name} does not support operation ${node.toDocument().print()}.")
