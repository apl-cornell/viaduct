package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host

class HostCommunicationException(
    val host: Host,
    val otherHost: Host
) : ViaductRuntimeException(
    if (host == otherHost) {
        "Runtime for host ${host.name} cannot send/receive data to/from itself"
    } else {
        "Runtime for host ${host.name} cannot send/receive data to/from host ${otherHost.name}"
    }
)
