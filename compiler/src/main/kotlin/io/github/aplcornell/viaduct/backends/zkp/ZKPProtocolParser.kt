package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.HostValue

/** Parser for the [ZKP] protocol. */
object ZKPProtocolParser : ProtocolParser<ZKP> {
    override fun parse(arguments: ProtocolArguments): ZKP {
        val sender = arguments.get<HostValue>("prover")
        val receivers =
            arguments.getAndAlso<HostSetValue>("verifiers") {
                if (it.isEmpty()) {
                    throw IllegalArgumentException("verifiers cannot be empty")
                }
                if (sender.value in it) {
                    throw IllegalArgumentException("verifiers cannot contain prover")
                }
            }
        return ZKP(sender.value, receivers)
    }
}
