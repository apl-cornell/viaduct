package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol

interface ProtocolComposer {
    fun communicate(src: Protocol, dst: Protocol): ProtocolCommunication
}
