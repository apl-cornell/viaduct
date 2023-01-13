package io.github.aplcornell.viaduct.runtime

import io.github.aplcornell.viaduct.syntax.Host

class HostCommunicationException(host: Host, otherHost: Host) : ViaductRuntimeException(
    if (host == otherHost) {
        "Runtime for host ${host.name} cannot send/receive data to/from itself."
    } else {
        "Runtime for host ${host.name} cannot send/receive data to/from host ${otherHost.name}."
    },
)
