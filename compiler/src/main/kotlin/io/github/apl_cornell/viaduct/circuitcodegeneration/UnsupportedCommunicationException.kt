package io.github.apl_cornell.viaduct.circuitcodegeneration

import io.github.apl_cornell.viaduct.prettyprinting.DefaultStyle
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.SourceLocation

/**
 * Thrown when a protocol is assigned an operator it cannot implement.
 * Usually indicates a bug in protocol selection.
 */
class UnsupportedCommunicationException(from: Protocol, to: Protocol, sourceLocation: SourceLocation) :
    CodeGenerationException(
        "Protocol ${from.name} does not support communication to ${to.name}.\n ${
        sourceLocation.showInSource(
            DefaultStyle
        ).print()
        }"
    )
