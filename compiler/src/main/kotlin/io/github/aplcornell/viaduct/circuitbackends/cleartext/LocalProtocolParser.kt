package io.github.aplcornell.viaduct.circuitbackends.cleartext

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostValue

/** Parser for the [Local] protocol. */
object LocalProtocolParser : ProtocolParser<Local> {
    override fun parse(arguments: ProtocolArguments): Local {
        val host = arguments.get<HostValue>("host")
        return Local(host.value)
    }
}
