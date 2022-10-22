package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.HostValue

/** Parser for the [ZKP] protocol. */
object ZKPProtocolParser : ProtocolParser<ZKP> {
    override fun parse(arguments: ProtocolArguments): ZKP {
        val sender = arguments.get<HostValue>("prover")
        val receivers = arguments.get<HostSetValue>("verifiers")
        return ZKP(sender.value, receivers)
    }
}
