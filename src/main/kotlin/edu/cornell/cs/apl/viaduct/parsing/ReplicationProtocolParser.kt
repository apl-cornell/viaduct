package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue

/** Parser for the [Replication] protocol. */
object ReplicationProtocolParser : ProtocolParser<Replication> {
    override fun parse(arguments: ProtocolArguments): Replication {
        val hosts = arguments.get<HostSetValue>("hosts")
        return Replication(hosts)
    }
}
