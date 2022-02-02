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
import java.io.File
import java.util.Scanner

class FileIOStrategy(inFile: File) : IOStrategy {
    private val fileScanner = Scanner(inFile)

    override fun input(type: IOValueType): Value {
        return when (type) {
            is IntegerType -> {
                IntegerValue(fileScanner.nextInt())
            }

            is BooleanType -> {
                BooleanValue(fileScanner.nextBoolean())
            }

            is UnitType -> {
                fileScanner.next("unit")
                UnitValue
            }
        }
    }

    override fun output(value: IOValue) {
        println(value)
    }
}
