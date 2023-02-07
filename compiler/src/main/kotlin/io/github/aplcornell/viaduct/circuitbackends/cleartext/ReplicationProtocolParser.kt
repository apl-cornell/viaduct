package io.github.aplcornell.viaduct.circuitbackends.cleartext

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostSetValue

/** Parser for the [Replication] protocol. */
object ReplicationProtocolParser : ProtocolParser<Replication> {
    override fun parse(arguments: ProtocolArguments): Replication {
        val hosts = arguments.get<HostSetValue>("hosts")
        return Replication(hosts)
    }
}
