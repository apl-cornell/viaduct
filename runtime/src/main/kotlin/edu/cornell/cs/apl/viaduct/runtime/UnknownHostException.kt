package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host

class UnknownHostException(
    val host: Host
) : ViaductRuntimeException("Unknown host ${host.name}")
