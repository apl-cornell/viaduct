package edu.cornell.cs.apl.viaduct.security.solver

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstraintSystem
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Label.Companion.fromConfidentiality
import edu.cornell.cs.apl.viaduct.security.Label.Companion.fromIntegrity
import edu.cornell.cs.apl.viaduct.security.Principal
import java.io.Writer

typealias ConstraintSolution = Map<LabelVariable, Label>

/**
 * Given a set of information flow constraints, finds a label assignment to all variables that
 * minimizes the trust assigned to each variable (if one exists).
 *
 * @param T type of exceptions thrown when there are unsatisfiable constraints
 */
class ConstraintSolver<T : Throwable> {
    private val constraintSystem = ConstraintSystem<FreeDistributiveLattice<Principal>, T>(
        FreeDistributiveLattice.bounds<Principal>().top
    )

    /** The set of variables that appear in the constraints. */
    private val variables: MutableSet<LabelVariable> = HashSet()

    /** Return the number of variables in the constraint system. */
    fun variableCount(): Int {
        return variables.size
    }

    /**
     * Find the least trust solution to the set of constraints in the system.
     *
     * @return mapping from variables to minimal trust labels that satisfy all constraints
     * @throws T if there are unsatisfiable constraints
     */
    fun solve(): ConstraintSolution {
        val componentSolutions = constraintSystem.solve()
        val solutions: MutableMap<LabelVariable, Label> = HashMap()
        for (variable in variables) {
            val confidentiality = variable.confidentialityComponent.getValue(componentSolutions)
            val integrity = variable.integrityComponent.getValue(componentSolutions)
            solutions[variable] = Label(confidentiality, integrity)
        }
        return solutions
    }

    /**
     * Create a fresh variable and add it to the system.
     *
     * @param label an arbitrary object to use as a label during debugging
     * @return the freshly created variable
     */
    fun addNewVariable(label: Any): LabelVariable {
        val variable = LabelVariable(
            constraintSystem.addNewVariable(ConfidentialityWrapper(label)),
            constraintSystem.addNewVariable(IntegrityWrapper(label))
        )
        variables.add(variable)
        return variable
    }

    /**
     * Add the constraint `lhs.flowsTo(rhs)` to the system.
     *
     * @param failWith a function that generates the exception to throw if this constraint is
     * unsatisfiable. The function will be given the best estimates for the values of `lhs`
     * and `rhs`.
     */
    fun addFlowsToConstraint(lhs: AtomicLabelTerm, rhs: LabelTerm, failWith: (from: Label, to: Label) -> T) {
        addConfidentialityFlowsToConstraint(lhs, rhs, failWith)
        addIntegrityFlowsToConstraint(lhs, rhs, failWith)
    }

    /**
     * Add the constraint `lhs == rhs` to the system.
     *
     * @param failWith same as in [addFlowsToConstraint]
     */
    fun addEqualToConstraint(lhs: AtomicLabelTerm, rhs: AtomicLabelTerm, failWith: (from: Label, to: Label) -> T) {
        addFlowsToConstraint(lhs, rhs, failWith)
        addFlowsToConstraint(rhs, lhs) { to: Label, from: Label -> failWith(from, to) }
    }

    /** Output the constraints as a DOT graph.  */
    fun exportDotGraph(writer: Writer?) {
        constraintSystem.exportDotGraph(writer)
    }

    /**
     * Add the constraint `lhs.confidentiality().flowsTo(rhs.confidentiality())`.
     *
     * @param failWith same as in [addFlowsToConstraint]
     */
    private fun addConfidentialityFlowsToConstraint(
        lhs: AtomicLabelTerm,
        rhs: LabelTerm,
        failWith: (from: Label, to: Label) -> T
    ) {
        constraintSystem.addLessThanOrEqualToConstraint(
            rhs.confidentialityComponent,
            lhs.confidentialityComponent
        ) { to, from -> failWith(fromConfidentiality(from), fromConfidentiality(to)) }
    }

    /**
     * Add the constraint `lhs.integrity().flowsTo(rhs.integrity())`.
     *
     * @param failWith same as in [addFlowsToConstraint]
     */
    private fun addIntegrityFlowsToConstraint(
        lhs: AtomicLabelTerm,
        rhs: LabelTerm,
        failWith: (from: Label, to: Label) -> T
    ) {
        constraintSystem.addLessThanOrEqualToConstraint(
            lhs.integrityComponent,
            rhs.integrityComponent
        ) { from, to -> failWith(fromIntegrity(from), fromIntegrity(to)) }
    }

    /** Label wrapper for variables that track confidentiality.  */
    private class ConfidentialityWrapper(private val label: Any) {
        override fun toString(): String {
            return "(c) $label"
        }
    }

    /** Label wrapper for variables that track integrity.  */
    private class IntegrityWrapper(private val label: Any) {
        override fun toString(): String {
            return "(i) $label"
        }
    }
}
