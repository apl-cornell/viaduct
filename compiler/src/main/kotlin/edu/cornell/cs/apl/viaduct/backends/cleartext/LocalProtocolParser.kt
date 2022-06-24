package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.parsing.ProtocolArguments
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue

/** Parser for the [Local] protocol. */
object LocalProtocolParser : ProtocolParser<Local> {
    override fun parse(arguments: ProtocolArguments): Local {
        val host = arguments.get<HostValue>("host")
        return Local(host.value)
    }
}
