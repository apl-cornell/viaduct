package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class CommitmentBackend : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, program: ProgramNode, process: BlockNode) {
        when (runtime.projection.protocol) {
            is Commitment ->
                if (runtime.projection.host == runtime.projection.protocol.cleartextHost) {
                    CommitmentCleartext(
                        program,
                        runtime,
                        runtime.projection.protocol.hashHosts
                    ).run(process)
                } else {
                    CommitmentHashReplica(
                        program,
                        runtime,
                        runtime.projection.protocol.cleartextHost
                    ).run(process)
                }
            else ->
                throw ViaductInterpreterError("CommitmentBackend: unexpected runtime protocol")
        }
    }
}
