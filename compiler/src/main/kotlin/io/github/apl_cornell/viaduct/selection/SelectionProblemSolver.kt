package io.github.apl_cornell.viaduct.selection

interface SelectionProblemSolver {
    val solverName: String
    fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment?
}
