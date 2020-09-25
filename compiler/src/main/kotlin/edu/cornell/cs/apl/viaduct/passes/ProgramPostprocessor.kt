package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

interface ProgramPostprocessor {
    fun postprocess(splitProgram: ProgramNode): ProgramNode
}
