package io.github.aplcornell.viaduct.selection

interface SelectionProblemSolver {
    fun solve(problem: SelectionProblem): ProtocolAssignment?
}
