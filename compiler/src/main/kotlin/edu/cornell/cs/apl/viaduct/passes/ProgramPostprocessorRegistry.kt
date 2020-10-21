package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class ProgramPostprocessorRegistry(
    private val postprocessors: List<ProgramPostprocessor>
) : ProgramPostprocessor {

    constructor(vararg postprocessors: ProgramPostprocessor) :
        this(listOf(*postprocessors))

    override fun postprocess(program: ProgramNode): ProgramNode {
        var currentProgram = program
        for (postprocessor in postprocessors) {
            currentProgram = postprocessor.postprocess(currentProgram)
        }
        return currentProgram
    }
}
