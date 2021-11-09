package edu.cornell.cs.apl.viaduct.selection

interface SelectionProblemSolver {
    fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment?
}
