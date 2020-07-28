package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

interface ProtocolBackend {
    fun initialize(connectionMap: Map<Host, HostAddress>, projection: ProtocolProjection) { }

    suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode)
}
