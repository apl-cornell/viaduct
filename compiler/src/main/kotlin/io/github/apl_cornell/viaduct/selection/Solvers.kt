package io.github.apl_cornell.viaduct.selection

/** Returns all available [SelectionProblemSolver] instances. */
val selectionProblemSolvers: List<Pair<String, SelectionProblemSolver>> =
    listOfNotNull(
        Pair("z3", Z3SelectionProblemSolver),
    )

val defaultSelectionProblemSolver: SelectionProblemSolver =
    selectionProblemSolvers.first().second
