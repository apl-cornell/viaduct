package edu.cornell.cs.apl.viaduct.selection

interface SelectionConstraintSolver {
    fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment
}
