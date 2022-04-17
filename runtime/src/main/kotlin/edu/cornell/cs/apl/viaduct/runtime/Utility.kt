package edu.cornell.cs.apl.viaduct.runtime

import java.net.ServerSocket

fun findAvailableTcpPort() = ServerSocket(0).use { it.localPort }
