package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

class CommitmentBackend(
    val typeAnalysis: TypeAnalysis
) : ProtocolBackend {
    override suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode) {
        TODO("Not yet implemented")
    }
}
