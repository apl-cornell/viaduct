package io.github.aplcornell.viaduct.runtime

import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.IOValueType
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.types.UnitType
import io.github.aplcornell.viaduct.syntax.values.BooleanValue
import io.github.aplcornell.viaduct.syntax.values.IOValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.UnitValue
import io.github.aplcornell.viaduct.syntax.values.Value
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
