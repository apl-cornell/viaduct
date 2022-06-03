package io.github.apl_cornell.viaduct.selection

interface SelectionProblemSolver {
    fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment?
}
