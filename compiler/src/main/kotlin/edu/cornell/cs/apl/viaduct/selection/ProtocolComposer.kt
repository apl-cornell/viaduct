package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

interface ProtocolComposer {
    fun communicate(src: Protocol, dst: Protocol): ProtocolCommunication
    fun mandatoryParticipatingHosts(protocol: Protocol, stmt: SimpleStatementNode): Set<Host>
}
