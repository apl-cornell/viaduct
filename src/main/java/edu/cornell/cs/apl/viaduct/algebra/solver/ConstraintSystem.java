package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.algebra.PartialOrder;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import io.vavr.Function2;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.IntegerComponentNameProvider;

/**
 * Given a set of constraints of the form {@code t1 ≤ t2}, finds the unique maximum solution if it
 * exists.
 *
 * <p>A solution to a set of constraints is an assignment of values to all variables in the system.
 * A maximum solution assigns the greatest possible value to each variable, where greatest is with
 * respect to {@link PartialOrder#lessThanOrEqualTo(Object)}.
 *
 * @param <A> domain of values
 * @param <T> type of exceptions thrown when there are unsatisfiable constraints
 */
public final class ConstraintSystem<A extends HeytingAlgebra<A>, T extends Throwable> {
  /**
   * Maintains constraints as a graph. Each vertex corresponds to an atomic term (a variable or a
   * constant), and an edge from term {@code t2} to {@code t1} corresponds to the constraint {@code
   * t1 ≤ f(t2)} where {@code f} is a function determined by the edge.
   *
   * <p>Note that edges go from the greater vertexes to the lower vertexes since we are solving for
   * the greatest solution.
   */
  private final DirectedPseudograph<AtomicTerm<A>, DataFlowEdge<A>> constraints =
      new DirectedPseudograph<>(null);

  /**
   * Maps each constraint (i.e. edge) to the exception to be thrown if that edge turns out to be
   * unsatisfiable.
   */
  private final Map<DataFlowEdge<A>, Function2<A, A, T>> failures = new HashMap<>();

  /** Greatest element of {@code A}. */
  private final A top;

  /**
   * Create a new constraint system.
   *
   * @param top greatest element of {@code A}
   */
  public ConstraintSystem(A top) {
    this.top = top;
  }

  /**
   * Find the greatest solution to the set of constraints in the system.
   *
   * @throws T if there are unsatisfiable constraints
   * @return mapping from variables to the greatest values that satisfy all constraints
   */
  public Map<VariableTerm<A>, A> solve() throws T {
    // Use data flow analysis to find a solution for all nodes.
    final Map<AtomicTerm<A>, A> solutions =
        DataFlow.solve(top, constraints)
            .getOrElseThrow(
                (error) -> {
                  final DataFlowEdge<A> edge = error.getUnsatisfiableEdge();
                  final A lhs =
                      error.getSuboptimalAssignments().get(constraints.getEdgeSource(edge));
                  final A rhs =
                      error.getSuboptimalAssignments().get(constraints.getEdgeTarget(edge));
                  return failures.get(edge).apply(lhs, rhs);
                });

    // Only return solutions for nodes that correspond to variables.
    final Map<VariableTerm<A>, A> variableSolutions = new HashMap<>();
    for (Map.Entry<AtomicTerm<A>, A> entry : solutions.entrySet()) {
      if (entry.getKey() instanceof VariableTerm) {
        variableSolutions.put(((VariableTerm<A>) entry.getKey()), entry.getValue());
      }
    }

    return variableSolutions;
  }

  /**
   * Create a fresh variable and add it to the system.
   *
   * @param label an arbitrary object to use as a label during debugging
   * @return the freshly created variable
   */
  public VariableTerm<A> addNewVariable(Object label) {
    final VariableTerm<A> term = new VariableTerm<>(this.top, label);
    this.constraints.addVertex(term);
    return term;
  }

  /**
   * Add the constraint {@code lhs <= rhs} to the system.
   *
   * @param failWith a function that generates the exception to throw if this constraint is
   *     unsatisfiable. The function will be given the best estimates for the values of {@code lhs}
   *     and {@code rhs}.
   */
  public void addLessThanOrEqualToConstraint(
      AtomicTerm<A> lhs, RightHandTerm<A> rhs, Function2<A, A, T> failWith) {
    final DataFlowEdge<A> edge = rhs.getOutEdge();
    addEdge(rhs.getNode(), lhs, edge);
    failures.put(edge, failWith);
  }

  /**
   * Add the constraint {@code lhs <= rhs} to the system.
   *
   * @param failWith same as in {@link #addLessThanOrEqualToConstraint(AtomicTerm, RightHandTerm,
   *     Function2)}
   */
  public void addLessThanOrEqualToConstraint(
      LeftHandTerm<A> lhs, AtomicTerm<A> rhs, Function2<A, A, T> failWith) {
    final DataFlowEdge<A> edge = lhs.getInEdge();
    addEdge(rhs, lhs.getNode(), edge);
    failures.put(edge, failWith);
  }

  /**
   * Add the given vertex to the constraint graph, but only if it's a constant.
   *
   * <p>Variable vertexes are added when they are created, and we do not want to add them
   * automatically in case the programmer passes in variables created for a different constraint
   * system.
   */
  private void addVertexIfConstant(AtomicTerm<A> vertex) {
    if (vertex instanceof ConstantTerm) {
      constraints.addVertex(vertex);
    }
  }

  /**
   * Add {@code edge} between {@code source} and {@code destination}, automatically adding the
   * vertexes to the graphs if necessary.
   */
  private void addEdge(AtomicTerm<A> source, AtomicTerm<A> destination, DataFlowEdge<A> edge) {
    addVertexIfConstant(source);
    addVertexIfConstant(destination);
    constraints.addEdge(source, destination, edge);
  }

  /** Output the constraint system as a DOT graph. */
  public void exportDotGraph(Writer writer) {
    // TODO: add solutions to the graph.
    new DOTExporter<AtomicTerm<A>, DataFlowEdge<A>>(
            new IntegerComponentNameProvider<>(), AtomicTerm::toString, DataFlowEdge::toString)
        .exportGraph(constraints, writer);
  }
}
