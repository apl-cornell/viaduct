package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.Host

class HostCommunicationException(host: Host, otherHost: Host) : ViaductRuntimeException(
    if (host == otherHost) {
        "Runtime for host ${host.name} cannot send/receive data to/from itself."
    } else {
        "Runtime for host ${host.name} cannot send/receive data to/from host ${otherHost.name}."
    }
)
