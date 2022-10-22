package io.github.aplcornell.viaduct.runtime

import java.net.ServerSocket

fun findAvailableTcpPort() = ServerSocket(0).use { it.localPort }
