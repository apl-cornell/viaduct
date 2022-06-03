package io.github.apl_cornell.viaduct.backends.zkp

import io.github.apl_cornell.viaduct.parsing.ProtocolArguments
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.values.HostSetValue
import io.github.apl_cornell.viaduct.syntax.values.HostValue

/** Parser for the [ZKP] protocol. */
object ZKPProtocolParser : ProtocolParser<ZKP> {
    override fun parse(arguments: ProtocolArguments): ZKP {
        val sender = arguments.get<HostValue>("prover")
        val receivers = arguments.get<HostSetValue>("verifiers")
        return ZKP(sender.value, receivers)
    }
}
