package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue

/** Parser for the [ABY] protocol. */
object AbyProtocolParser : ProtocolParser<ABY> {
    override fun parse(arguments: ProtocolArguments): ABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        return ABY(server.value, client.value)
    }
}
