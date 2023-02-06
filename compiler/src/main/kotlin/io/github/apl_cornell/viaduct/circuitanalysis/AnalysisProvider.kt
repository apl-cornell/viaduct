package io.github.apl_cornell.viaduct.circuitanalysis

import io.github.apl_cornell.viaduct.syntax.circuit.ProgramNode

interface AnalysisProvider<Analysis> {
    /**
     * Returns the [Analysis] instance for [program].
     * The returned instance is cached for efficiency, so calling [get] again on [program] will
     * return the same instance.
     */
    fun get(program: ProgramNode): Analysis
}
