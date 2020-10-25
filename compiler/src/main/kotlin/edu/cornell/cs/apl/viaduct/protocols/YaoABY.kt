package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName

class YaoABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("YaoABY")
    }

    override val protocolName: ProtocolName
        get() = YaoABY.protocolName
}
