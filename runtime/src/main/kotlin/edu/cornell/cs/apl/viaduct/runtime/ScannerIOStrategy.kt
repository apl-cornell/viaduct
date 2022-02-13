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
import java.io.Writer
import java.nio.charset.Charset
import java.util.Scanner

class ScannerIOStrategy(
    private val input: Scanner,
    private val output: Writer = System.out.writer(Charset.defaultCharset())
) : IOStrategy {
    override fun input(type: IOValueType): Value {
        return when (type) {
            is BooleanType ->
                BooleanValue(input.nextBoolean())

            is IntegerType ->
                IntegerValue(input.nextInt())

            is UnitType -> {
                input.next(UnitType.toString())
                UnitValue
            }
        }
    }

    override fun output(value: IOValue) {
        output.write("$value\n")
    }
}
