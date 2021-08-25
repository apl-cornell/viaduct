package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

interface CodeGeneratorContext {
    val program: ProgramNode

    // returns a kotlin name for a temporary used in the source program
    fun kotlinName(sourceName: Temporary, protocol: Protocol): String

    fun kotlinName(sourceName: ObjectVariable): String

    // returns a fresh kotlin name for baseName
    fun newTemporary(baseName: String): String
}
