package io.github.apl_cornell.viaduct.backend.zkp

import io.github.apl_cornell.viaduct.backend.WireGenerator
import io.github.apl_cornell.viaduct.backend.WireTerm
import io.github.apl_cornell.viaduct.syntax.values.BooleanValue
import io.github.apl_cornell.viaduct.syntax.values.IntegerValue
import io.github.apl_cornell.viaduct.syntax.values.Value

sealed class ZKPObject {
    data class ZKPImmutableCell(val value: WireTerm) : ZKPObject()
    data class ZKPMutableCell(var value: WireTerm) : ZKPObject()
    class ZKPVectorObject(val size: Int, val defaultValue: Value, val wireGenerator: WireGenerator) : ZKPObject() {
        val gates: ArrayList<WireTerm> = ArrayList(size)

        init {

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

            for (i: Int in 0 until size) {
                gates.add(wireGenerator.mkConst(v.value))
            }
        }
    }

    object ZKPNullObject : ZKPObject()
}
