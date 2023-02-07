package io.github.aplcornell.viaduct.circuitcodegeneration

import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.Node

/**
 * Thrown when a protocol is assigned an operator it cannot implement.
 * Usually indicates a bug in protocol selection.
 */
class UnsupportedOperatorException(protocol: Protocol, node: Node) :
    CodeGenerationException("Protocol ${protocol.name} does not support operation ${node.toDocument().print()}.")
