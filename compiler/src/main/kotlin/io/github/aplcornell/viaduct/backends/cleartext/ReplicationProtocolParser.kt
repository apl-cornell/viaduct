package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostSetValue

/** Parser for the [Replication] protocol. */
object ReplicationProtocolParser : ProtocolParser<Replication> {
    override fun parse(arguments: ProtocolArguments): Replication {
        val hosts = arguments.getAndAlso<HostSetValue>("hosts") {
            if (it.size < 2) {
                throw IllegalArgumentException("need at least 2 hosts")
            }
        }
        return Replication(hosts)
    }
}
