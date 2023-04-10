package io.github.aplcornell.viaduct.circuitcodegeneration

import io.github.aplcornell.viaduct.prettyprinting.DefaultStyle
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.SourceLocation

/**
 * Thrown when a protocol is assigned an operator it cannot implement.
 * Usually indicates a bug in protocol selection.
 */
class UnsupportedCommunicationException(from: Protocol, to: Protocol, sourceLocation: SourceLocation) :
    CodeGenerationException(
        "Protocol ${from.name} does not support communication to ${to.name}.\n ${
            sourceLocation.showInSource(
                DefaultStyle,
            ).print()
        }",
    )
