package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

interface ProgramPostprocessor {
    fun postprocess(program: ProgramNode): ProgramNode
}
