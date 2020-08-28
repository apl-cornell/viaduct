package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

class CommitmentBackend(
    val typeAnalysis: TypeAnalysis
) : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode) {
        when (runtime.projection.protocol) {
            is Commitment ->
                if (runtime.projection.host == runtime.projection.protocol.cleartextHost) {
                    CommitmentCleartext(
                        runtime, typeAnalysis,
                        runtime.projection.protocol.receivers
                    ).run(process)
                } else {
                    CommitmentHashReplica(
                        runtime,
                        typeAnalysis,
                        runtime.projection.protocol.cleartextHost
                    ).run(process)
                }
            else ->
                throw ViaductInterpreterError("CommitmentBackend: unexpected runtime protocol")
        }
    }
}
