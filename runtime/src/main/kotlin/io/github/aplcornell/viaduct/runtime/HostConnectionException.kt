package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.Host
import java.net.InetSocketAddress

/** Thrown when the runtime fails to establish a connection to a host. */
class HostConnectionException(
    host: Host,
    otherHost: Host,
    address: InetSocketAddress
) : ViaductRuntimeException(
    "Runtime for host ${host.name} cannot connect to host ${otherHost.name} at $address."
)
