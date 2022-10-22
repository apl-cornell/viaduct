package io.github.aplcornell.viaduct.runtime

import io.github.aplcornell.viaduct.syntax.Host

class UnknownHostException(host: Host) : ViaductRuntimeException("Unknown host ${host.name}.")
