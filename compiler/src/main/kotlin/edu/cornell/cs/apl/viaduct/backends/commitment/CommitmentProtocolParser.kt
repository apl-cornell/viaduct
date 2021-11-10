package edu.cornell.cs.apl.viaduct.backends.commitment

import edu.cornell.cs.apl.viaduct.parsing.ProtocolArguments
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue

/** Parser for the [Commitment] protocol. */
object CommitmentProtocolParser : ProtocolParser<Commitment> {
    override fun parse(arguments: ProtocolArguments): Commitment {
        val sender = arguments.get<HostValue>("sender")
        val receivers = arguments.get<HostSetValue>("receivers")
        return Commitment(sender.value, receivers)
    }
}
