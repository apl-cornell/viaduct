package io.github.apl_cornell.viaduct.selection

interface SelectionProblemSolver {
    fun solve(problem: SelectionProblem): ProtocolAssignment?
}
