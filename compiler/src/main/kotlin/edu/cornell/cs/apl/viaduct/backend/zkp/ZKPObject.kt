package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.WireGenerator
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

sealed class ZKPObject {
    data class ZKPImmutableCell(val value: WireTerm) : ZKPObject()
    data class ZKPMutableCell(var value: WireTerm) : ZKPObject()
    class ZKPVectorObject(val size: Int, val defaultValue: Value, val wireGenerator: WireGenerator) : ZKPObject() {
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
                gates[i] = wireGenerator.mkConst(v.value)
            }
        }
    }
    object ZKPNullObject : ZKPObject()
}
