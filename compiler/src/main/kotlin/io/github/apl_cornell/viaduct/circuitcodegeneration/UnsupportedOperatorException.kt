package io.github.apl_cornell.viaduct.circuitcodegeneration

import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.circuit.Node

/**
 * Thrown when a protocol is assigned an operator it cannot implement.
 * Usually indicates a bug in protocol selection.
 */
class UnsupportedOperatorException(protocol: Protocol, node: Node) :
    CodeGenerationException("Protocol ${protocol.name} does not support operation ${node.toDocument().print()}.")
