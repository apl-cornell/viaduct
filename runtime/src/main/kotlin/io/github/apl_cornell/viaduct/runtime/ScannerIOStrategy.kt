package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.types.BooleanType
import io.github.apl_cornell.viaduct.syntax.types.IOValueType
import io.github.apl_cornell.viaduct.syntax.types.IntegerType
import io.github.apl_cornell.viaduct.syntax.types.UnitType
import io.github.apl_cornell.viaduct.syntax.values.BooleanValue
import io.github.apl_cornell.viaduct.syntax.values.IOValue
import io.github.apl_cornell.viaduct.syntax.values.IntegerValue
import io.github.apl_cornell.viaduct.syntax.values.UnitValue
import io.github.apl_cornell.viaduct.syntax.values.Value
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
