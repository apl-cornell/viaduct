package io.github.apl_cornell.viaduct.backends.cleartext

import io.github.apl_cornell.viaduct.parsing.ProtocolArguments
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.values.HostValue

/** Parser for the [Local] protocol. */
object LocalProtocolParser : ProtocolParser<Local> {
    override fun parse(arguments: ProtocolArguments): Local {
        val host = arguments.get<HostValue>("host")
        return Local(host.value)
    }
}
