package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue

/** Parser for the [ZKP] protocol. */
object ZKPProtocolParser : ProtocolParser<ZKP> {
    override fun parse(arguments: ProtocolArguments): ZKP {
        val sender = arguments.get<HostValue>("prover")
        val receivers = arguments.get<HostSetValue>("verifiers")
        return ZKP(sender.value, receivers)
    }
}
