package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IOValueType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IOValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Scanner

class TerminalIOStrategy : IOStrategy {
    private val stdinScanner: Scanner = Scanner(System.`in`)

    override fun input(type: IOValueType): Value {
        println("Input: ")
        return when (type) {
            is BooleanType -> BooleanValue(stdinScanner.nextBoolean())
            is IntegerType -> IntegerValue(stdinScanner.nextInt())
            is UnitType -> {
                stdinScanner.next("unit")
                UnitValue
            }
        }
    }

    override fun output(value: IOValue) {
        println(value)
    }
}
