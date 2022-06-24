package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.algebra.PartialOrder
import edu.cornell.cs.apl.viaduct.util.Colors
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge
import edu.cornell.cs.apl.viaduct.util.dataflow.solveDataFlow
import org.jgrapht.graph.DirectedMultigraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.Writer

typealias ConstraintSolution<A> = Map<VariableTerm<A>, A>

/**
 * Given a set of constraints of the form `t1 ≤ t2`, finds the unique maximum solution if it
 * exists.
 *
 * A solution to a set of constraints is an assignment of values to all variables in the system.
 * A maximum solution assigns the greatest possible value to each variable, where greatest is with
 * respect to [PartialOrder.lessThanOrEqualTo].
 *
 * @param A domain of values
 * @param T type of exceptions thrown when there are unsatisfiable constraints
 * @param top greatest element of [A]
 */
class ConstraintSystem<A : HeytingAlgebra<A>, T : Throwable>(private val top: A) {
    /**
     * Maintains constraints as a graph. Each vertex corresponds to an atomic term (a variable or a
     * constant), and an edge from term `t2` to `t1` corresponds to the constraint `t1 ≤ f(t2)` where
     * `f` is a function determined by the edge.
     *
     * Note that edges point from right-hand vertexes to the left-hand vertexes since we are
     * solving for the greatest solution.
     */
    private val constraints = DirectedMultigraph<AtomicTerm<A>, DataFlowEdge<A>>(null, null, false)

    /**
     * Maps each constraint (i.e. edge) to a function that generates the exception to be thrown if
     * that edge turns out to be unsatisfiable. The function is given a best-effort solution to the
     * constraints.
     */
    private val failures: MutableMap<DataFlowEdge<A>, (ConstraintSolution<A>) -> T> = mutableMapOf()

    /**
     * Returns the greatest solution to the set of constraints in the system.
     *
     * @return mapping from variables to the greatest values that satisfy all constraints
     * @throws T if there are unsatisfiable constraints
     */
    fun solve(): ConstraintSolution<A> {
        val solution = getConstraintSolution()

        // Verify the solution.
        for (edge in constraints.edgeSet()) {
            if (isConstraintViolated(edge, solution)) {
                throw failures.getValue(edge)(solution)
            }
        }

        return solution
    }

    /**
     * Returns the greatest solution to the set of constraints in the system.
     * Unlike [solve], does not throw an exception when there are unsatisfiable constraints.
     */
    private fun getConstraintSolution(): ConstraintSolution<A> {
        // Use data flow analysis to find a solution for all nodes.
        val solution: Map<AtomicTerm<A>, A> = solveDataFlow(top, constraints)

        // Restrict the mapping to only contain variable nodes.
        val variableAssignment = mutableMapOf<VariableTerm<A>, A>()
        for ((key, value) in solution) {
            if (key is VariableTerm<A>) {
                variableAssignment[key] = value
            }
        }

        return variableAssignment
    }

    /**
     * Creates a fresh variable and add it to the system.
     *
     * @param label an arbitrary object to use as a label during debugging
     * @return the freshly created variable
     */
    fun addNewVariable(label: Any): VariableTerm<A> {
        val variable = VariableTerm<A>(label)
        // Add the variable to the graph, so we return a solution for it even if it doesn't appear in
        // any constraints.
        constraints.addVertex(variable)
        return variable
    }

    /**
     * Add the constraint `lhs <= rhs` to the system.
     *
     * @param failWith a function that generates the exception to throw if this constraint is
     * unsatisfiable. The function will be given the best estimates for the values of `lhs`
     * and `rhs`.
     */
    fun addLessThanOrEqualToConstraint(lhs: AtomicTerm<A>, rhs: RightHandTerm<A>, failWith: (from: A, to: A) -> T) {
        if (!isTriviallyTrue(lhs, rhs)) {
            val edge = rhs.outEdge
            addEdgeWithVertices(rhs.node, lhs, edge)
            failures[edge] = { solution -> failWith(lhs.getValue(solution), rhs.getValue(solution)) }
        }
    }

    /**
     * Add the constraint `lhs <= rhs` to the system.
     *
     * @param failWith same as in [addLessThanOrEqualToConstraint]
     */
    fun addLessThanOrEqualToConstraint(lhs: LeftHandTerm<A>, rhs: AtomicTerm<A>, failWith: (from: A, to: A) -> T) {
        if (!isTriviallyTrue(lhs, rhs)) {
            val edge = lhs.inEdge
            addEdgeWithVertices(rhs, lhs.node, edge)
            failures[edge] = { solution -> failWith(lhs.getValue(solution), rhs.getValue(solution)) }
        }
    }

    /**
     * Identifies constraints that universally hold and can be safely ignored. This is useful for
     * simplifying the constraint graph, so it is more readable when exported.
     */
    private fun isTriviallyTrue(lhs: LeftHandTerm<A>, rhs: RightHandTerm<A>): Boolean =
        when {
            lhs == rhs ->
                true
            lhs is ConstantTerm<A> && rhs is ConstantTerm<A> ->
                lhs.value.lessThanOrEqualTo(rhs.value)
            else -> false
        }

    /**
     * Add `edge` between `source` and `target`, automatically adding the vertexes
     * to the graphs if necessary.
     */
    private fun addEdgeWithVertices(source: AtomicTerm<A>, target: AtomicTerm<A>, edge: DataFlowEdge<A>) {
        constraints.addVertex(source)
        constraints.addVertex(target)
        constraints.addEdge(source, target, edge)
    }

    /**
     * Return `true` if the constraint represented by an edge is violated by the given solution.
     *
     * @param edge constraint to check
     * @param solution a (possibly invalid) solution to the constraints in the system
     */
    private fun isConstraintViolated(edge: DataFlowEdge<A>, solution: ConstraintSolution<A>): Boolean {
        val sourceValue = constraints.getEdgeSource(edge).getValue(solution)
        val targetValue = constraints.getEdgeTarget(edge).getValue(solution)
        return !targetValue.lessThanOrEqualTo(edge.propagate(sourceValue))
    }

    /** Output the constraint system as a DOT graph.  */
    fun exportDotGraph(writer: Writer?) {
        val solution = getConstraintSolution()
        val dotExporter = DOTExporter<AtomicTerm<A>, DataFlowEdge<A>>()

        // Vertex labels and shape
        dotExporter.setVertexAttributeProvider { vertex: AtomicTerm<A> ->
            val attributes = mutableMapOf<String, Attribute>()

            // Include solution in the label for variables
            val label =
                if (vertex is VariableTerm<A>)
                    "$vertex\n{${solution.getValue(vertex)}}"
                else
                    vertex.toString()
            attributes["label"] = DefaultAttribute.createAttribute(label)

            // Differentiate constant vertices from variable vertices
            attributes["color"] = DefaultAttribute.createAttribute(Colors.BLACK)
            attributes["fontcolor"] = DefaultAttribute.createAttribute(Colors.BLACK)
            if (vertex is ConstantTerm<A>) {
                attributes["color"] = DefaultAttribute.createAttribute(Colors.GRAY)
                attributes["style"] = DefaultAttribute.createAttribute("filled")
            }
            attributes
        }

        // Edge labels and shape
        dotExporter.setEdgeAttributeProvider { edge: DataFlowEdge<A> ->
            // Highlight edges that represent violated constraints
            val color =
                when {
                    isConstraintViolated(edge, solution) ->
                        Colors.RED
                    edge !is IdentityEdge<*> ->
                        Colors.BLUE
                    else ->
                        Colors.BLACK
                }
            mapOf(
                "label" to DefaultAttribute.createAttribute(edge.toString()),
                "color" to DefaultAttribute.createAttribute(color),
                "fontcolor" to DefaultAttribute.createAttribute(color)
            )
        }

        dotExporter.exportGraph(constraints, writer)
    }
}
