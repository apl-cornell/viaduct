package io.github.apl_cornell.viaduct.backends.commitment

import io.github.apl_cornell.viaduct.parsing.ProtocolArguments
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.values.HostSetValue
import io.github.apl_cornell.viaduct.syntax.values.HostValue

/** Parser for the [Commitment] protocol. */
object CommitmentProtocolParser : ProtocolParser<Commitment> {
    override fun parse(arguments: ProtocolArguments): Commitment {
        val sender = arguments.get<HostValue>("sender")
        val receivers = arguments.get<HostSetValue>("receivers")
        return Commitment(sender.value, receivers)
    }
}
