package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.algebra.PartialOrder;
import edu.cornell.cs.apl.viaduct.util.Colors;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;
import io.vavr.Function2;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.ComponentAttributeProvider;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DefaultAttribute;
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
   * <p>Note that edges point from right-hand vertexes to the left-hand vertexes since we are
   * solving for the greatest solution.
   */
  private final DirectedMultigraph<AtomicTerm<A>, DataFlowEdge<A>> constraints =
      new DirectedMultigraph<>(null);

  /**
   * Maps each constraint (i.e. edge) to a function that generates the exception to be thrown if
   * that edge turns out to be unsatisfiable. The function is given a best-effort solution to the
   * constraints.
   */
  private final Map<DataFlowEdge<A>, Function<Map<VariableTerm<A>, A>, T>> failures =
      new HashMap<>();

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
   * @return mapping from variables to the greatest values that satisfy all constraints
   * @throws T if there are unsatisfiable constraints
   */
  public Map<VariableTerm<A>, A> solve() throws T {
    // Use data flow analysis to find a solution for all nodes.
    final Map<AtomicTerm<A>, A> solution = DataFlow.solve(top, constraints);

    // Restrict the mapping to only contain variable nodes.
    final Map<VariableTerm<A>, A> variableAssignment = new HashMap<>();
    for (Map.Entry<AtomicTerm<A>, A> entry : solution.entrySet()) {
      if (entry.getKey() instanceof VariableTerm) {
        variableAssignment.put(((VariableTerm<A>) entry.getKey()), entry.getValue());
      }
    }

    // Verify the solution.
    for (DataFlowEdge<A> edge : constraints.edgeSet()) {
      if (isConstraintViolated(edge, solution)) {
        throw failures.get(edge).apply(variableAssignment);
      }
    }

    return variableAssignment;
  }

  /**
   * Create a fresh variable and add it to the system.
   *
   * @param label an arbitrary object to use as a label during debugging
   * @return the freshly created variable
   */
  public VariableTerm<A> addNewVariable(Object label) {
    final VariableTerm<A> variable = new VariableTerm<>(label);
    // Add the variable to the graph so we return a solution for it even if it doesn't appear in
    // any constraints.
    constraints.addVertex(variable);
    return variable;
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
    if (isTriviallyTrue(lhs, rhs)) {
      return;
    }
    final DataFlowEdge<A> edge = rhs.getOutEdge();
    addEdgeWithVertices(rhs.getNode(), lhs, edge);
    failures.put(
        edge, (solution) -> failWith.apply(lhs.getValue(solution), rhs.getValue(solution)));
  }

  /**
   * Add the constraint {@code lhs <= rhs} to the system.
   *
   * @param failWith same as in {@link #addLessThanOrEqualToConstraint(AtomicTerm, RightHandTerm,
   *     Function2)}
   */
  public void addLessThanOrEqualToConstraint(
      LeftHandTerm<A> lhs, AtomicTerm<A> rhs, Function2<A, A, T> failWith) {
    if (isTriviallyTrue(lhs, rhs)) {
      return;
    }
    final DataFlowEdge<A> edge = lhs.getInEdge();
    addEdgeWithVertices(rhs, lhs.getNode(), edge);
    failures.put(
        edge, (solution) -> failWith.apply(lhs.getValue(solution), rhs.getValue(solution)));
  }

  /**
   * Identifies constraints that universally hold and can be safely ignored. This is useful for
   * simplifying the constraint graph so it is more readable when exported.
   */
  private boolean isTriviallyTrue(LeftHandTerm<A> lhs, RightHandTerm<A> rhs) {
    if (lhs.equals(rhs)) {
      return true;
    }
    if (lhs instanceof ConstantTerm && rhs instanceof ConstantTerm) {
      final A leftValue = ((ConstantTerm<A>) lhs).getValue();
      final A rightValue = ((ConstantTerm<A>) rhs).getValue();
      return leftValue.lessThanOrEqualTo(rightValue);
    }
    return false;
  }

  /**
   * Add {@code edge} between {@code source} and {@code target}, automatically adding the vertexes
   * to the graphs if necessary.
   */
  private void addEdgeWithVertices(
      AtomicTerm<A> source, AtomicTerm<A> target, DataFlowEdge<A> edge) {
    constraints.addVertex(source);
    constraints.addVertex(target);
    constraints.addEdge(source, target, edge);
  }

  /**
   * Return {@code true} if the constraint represented by an edge is violated by the given solution.
   *
   * @param edge constraint to check
   * @param solution a (possibly invalid) solution to the constraints in the system
   */
  private boolean isConstraintViolated(DataFlowEdge<A> edge, Map<AtomicTerm<A>, A> solution) {
    final A sourceValue = solution.get(constraints.getEdgeSource(edge));
    final A targetValue = solution.get(constraints.getEdgeTarget(edge));
    return !targetValue.lessThanOrEqualTo(edge.propagate(sourceValue));
  }

  /** Output the constraint system as a DOT graph. */
  public void exportDotGraph(Writer writer) {
    final Map<AtomicTerm<A>, A> solution = DataFlow.solve(top, constraints);

    final ComponentNameProvider<AtomicTerm<A>> vertexLabelProvider =
        (vertex) -> {
          if (vertex instanceof VariableTerm) {
            // Print solutions for variables
            return vertex.toString() + "\n" + "{" + solution.get(vertex) + "}";
          } else {
            return vertex.toString();
          }
        };

    // Differentiate constants from variables
    final ComponentAttributeProvider<AtomicTerm<A>> vertexAttributeProvider =
        (vertex) -> {
          final Map<String, Attribute> attributes = new HashMap<>();
          attributes.put("color", DefaultAttribute.createAttribute(Colors.BLACK));
          attributes.put("fontcolor", DefaultAttribute.createAttribute(Colors.BLACK));
          if (vertex instanceof ConstantTerm) {
            attributes.put("color", DefaultAttribute.createAttribute(Colors.GRAY));
            attributes.put("style", DefaultAttribute.createAttribute("filled"));
          }
          return attributes;
        };

    // Highlight violated constraints
    final ComponentAttributeProvider<DataFlowEdge<A>> edgeAttributeProvider =
        (edge) -> {
          final String color;
          if (isConstraintViolated(edge, solution)) {
            color = Colors.RED;
          } else if (!(edge instanceof IdentityEdge)) {
            color = Colors.BLUE;
          } else {
            color = Colors.BLACK;
          }

          return Map.of(
              "color",
              DefaultAttribute.createAttribute(color),
              "fontcolor",
              DefaultAttribute.createAttribute(color));
        };

    new DOTExporter<>(
            new IntegerComponentNameProvider<>(),
            vertexLabelProvider,
            DataFlowEdge::toString,
            vertexAttributeProvider,
            edgeAttributeProvider)
        .exportGraph(constraints, writer);
  }
}
