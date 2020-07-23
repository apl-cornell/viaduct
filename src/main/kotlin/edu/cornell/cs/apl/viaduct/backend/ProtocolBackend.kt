package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

interface ProtocolBackend {
    val supportedProtocols: Set<String>

    suspend fun run(
        nameAnalysis: NameAnalysis,
        typeAnalysis: TypeAnalysis,
        runtime: ViaductRuntime,
        program: ProgramNode,
        process: BlockNode,
        projection: ProtocolProjection
    )
}
