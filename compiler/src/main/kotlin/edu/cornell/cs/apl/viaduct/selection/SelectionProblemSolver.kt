package edu.cornell.cs.apl.viaduct.selection

interface SelectionProblemSolver {
    val solverName: String
    fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment?
}
