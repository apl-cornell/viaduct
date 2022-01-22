package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host

/** Thrown when the runtime fails to establish a connection to a host. */
class HostConnectionException(
    host: Host,
    otherHost: Host,
    address: HostAddress
) : ViaductRuntimeException(
    "Runtime for host ${host.name} cannot connect to host ${otherHost.name} at address $address"
)
