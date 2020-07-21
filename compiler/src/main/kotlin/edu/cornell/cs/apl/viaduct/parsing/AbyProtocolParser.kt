package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue

/** Parser for the [ABY] protocol. */
object AbyProtocolParser : ProtocolParser<ABY> {
    override fun parse(arguments: ProtocolArguments): ABY {
        val hosts = arguments.get<HostSetValue>("hosts")
        return ABY(hosts)
    }
}
