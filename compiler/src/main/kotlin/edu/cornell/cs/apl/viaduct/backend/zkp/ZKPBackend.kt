package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.WireConst
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class ZKPBackend : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, program: ProgramNode, process: BlockNode) {
        when (runtime.projection.protocol) {
            is ZKP ->
                if (runtime.projection.host == runtime.projection.protocol.prover) {
                    ZKPProver(
                        program,
                        runtime,
                        runtime.projection.protocol.verifiers
                    ).run(process)
                } else {
                    ZKPVerifier(
                        program,
                        runtime,
                        runtime.projection.protocol.prover,
                        runtime.projection.protocol.verifiers
                    ).run(process)
                }
            else ->
                throw ViaductInterpreterError("CommitmentBackend: unexpected runtime protocol")
        }
    }
}

sealed class ZKPObject {
    data class ZKPImmutableCell(val value: WireTerm) : ZKPObject()
    data class ZKPMutableCell(var value: WireTerm) : ZKPObject()
    class ZKPVectorObject(val size: Int, val defaultValue: Value) : ZKPObject() {
        val gates: ArrayList<WireTerm> = ArrayList(size)

        init {
            for (i: Int in 0 until size) {
                val v: IntegerValue =
                    when (defaultValue) {
                        is IntegerValue -> defaultValue
                        is BooleanValue -> IntegerValue(
                            if (defaultValue.value) {
                                1
                            } else {
                                0
                            }
                        )
                        else -> throw Exception("Bad default value")
                    }
                gates[i] = WireConst(v.value)
            }
        }
    }
    object ZKPNullObject : ZKPObject()
}
