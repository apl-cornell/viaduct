package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

interface ProgramPostprocessor {
    fun postprocess(program: ProgramNode): ProgramNode
}
