package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue

/** Parser for the [Local] protocol. */
object LocalProtocolParser : ProtocolParser<Local> {
    override fun parse(arguments: ProtocolArguments): Local {
        val host = arguments.get<HostValue>("host")
        return Local(host.value)
    }
}
