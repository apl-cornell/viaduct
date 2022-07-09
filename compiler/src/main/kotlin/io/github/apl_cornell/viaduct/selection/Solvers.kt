package io.github.apl_cornell.viaduct.selection

private val gurobiSolver: SelectionProblemSolver? =
    try {
        val gurobiClass = Class.forName("${PackageName.javaClass.packageName}.GurobiSelectionProblemSolver")
        val instance = gurobiClass.getDeclaredField("INSTANCE").get(null) as SelectionProblemSolver
        instance
    } catch (e: ClassNotFoundException) {
        null
    }

private object PackageName

/** Returns all available [SelectionProblemSolver] instances. */
val selectionProblemSolvers: List<Pair<String, SelectionProblemSolver>> =
    listOfNotNull(
        Pair("z3", Z3SelectionProblemSolver),
        gurobiSolver?.let { Pair("gurobi", it) }
    )

val defaultSelectionProblemSolver: SelectionProblemSolver =
    selectionProblemSolvers.first().second
