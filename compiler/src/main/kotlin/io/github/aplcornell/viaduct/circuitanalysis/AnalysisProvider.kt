package io.github.aplcornell.viaduct.circuitanalysis

import io.github.aplcornell.viaduct.syntax.circuit.ProgramNode

interface AnalysisProvider<Analysis> {
    /**
     * Returns the [Analysis] instance for [program].
     * The returned instance is cached for efficiency, so calling [get] again on [program] will
     * return the same instance.
     */
    fun get(program: ProgramNode): Analysis
}
