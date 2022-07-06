package io.github.apl_cornell.viaduct.selection

val defaultSelectionProblemSolver: SelectionProblemSolver =
    Z3SelectionProblemSolver

/** Returns a list name-constructor pairs for all [SelectionProblemSolver] classes. */
val selectionProblemSolvers: List<Pair<String, SelectionProblemSolver>> =
    listOfNotNull(
        Pair("z3", Z3SelectionProblemSolver),
        try {
            val gurobiClass = Class.forName("${PackageName.javaClass.packageName}.GurobiSelectionProblemSolver")
            val instance = gurobiClass.getDeclaredField("INSTANCE").get(null) as SelectionProblemSolver
            Pair("gurobi", instance)
        } catch (e: ClassNotFoundException) {
            null
        }
    )

private object PackageName
