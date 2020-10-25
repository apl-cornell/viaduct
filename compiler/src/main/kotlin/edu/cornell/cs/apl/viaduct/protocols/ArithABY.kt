package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName

class ArithABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("ArithABY")
    }

    override val protocolName: ProtocolName
        get() = ArithABY.protocolName
}
