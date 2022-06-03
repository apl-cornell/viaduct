package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.Host

class UnknownHostException(host: Host) : ViaductRuntimeException("Unknown host ${host.name}.")
