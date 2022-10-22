package io.github.aplcornell.viaduct.backends.commitment

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.HostValue

/** Parser for the [Commitment] protocol. */
object CommitmentProtocolParser : ProtocolParser<Commitment> {
    override fun parse(arguments: ProtocolArguments): Commitment {
        val sender = arguments.get<HostValue>("sender")
        val receivers = arguments.get<HostSetValue>("receivers")
        return Commitment(sender.value, receivers)
    }
}
