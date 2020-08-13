package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

interface ProtocolBackend {
    fun initialize(connectionMap: Map<Host, HostAddress>, projection: ProtocolProjection) { }

    suspend fun run(runtime: ViaductProcessRuntime, program: ProgramNode, process: BlockNode)
}
