package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class ZKPBackend : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, program: ProgramNode, process: BlockNode) {
        TODO()
    }
}
