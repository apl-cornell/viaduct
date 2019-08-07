package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.JoinSemiLattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.graph.DirectedPseudograph;

public class ConstraintSystem<A extends JoinSemiLattice<A>> {
  /**
   * Maintain constraints as a graph. Each vertex corresponds to an atomic term (a variable or a
   * constant), and an edge from term {@code t1} to {@code t2} corresponds to the constraint {@code
   * f(t1) <= t2} where {@code f} is a function determined by the edge.
   */
  private final DirectedPseudograph<ConstraintValue<A>, DataFlowEdge<A>> constraints =
      new DirectedPseudograph<>(null);

  /** Least element of {@code A}. */
  private final A bottom;

  /**
   * Create a new constraint system.
   *
   * @param bottom least element of type {@code A}
   */
  public ConstraintSystem(A bottom) {
    this.bottom = bottom;
  }

  /**
   * Find a least solution to the set of constraints in the system.
   *
   * @return Mapping from variables to the smallest values that satisfy all constraints
   */
  public Map<VariableTerm, A> solve() throws UnsatisfiableConstraintException {
    // Use data flow analysis to find a solution for all nodes.
    Map<ConstraintValue<A>, A> solutions =
        new DataFlow<A, UnsatisfiableConstraintException, ConstraintValue<A>, DataFlowEdge<A>>(
                bottom)
            .solve(constraints);

    // Only return solutions for nodes that correspond to variables.
    final Map<VariableTerm, A> variableSolutions = new HashMap<>();
    for (Map.Entry<ConstraintValue<A>, A> entry : solutions.entrySet()) {
      if (entry.getKey() instanceof ConstraintSystem.VariableTerm) {
        variableSolutions.put(((VariableTerm) entry.getKey()), entry.getValue());
      }
    }

    return variableSolutions;
  }

  /** Create a fresh variable and add it to the system. */
  public VariableTerm addNewVariable() {
    return new VariableTerm();
  }

  /** Add the constraint {@code lhs <= rhs} to the system. */
  public void addLessThanOrEqualToConstraint(LeftHandTerm<A> lhs, RightHandTerm<A> rhs) {
    if (rhs instanceof ConstraintValue) {
      constraints.addEdge(lhs.getNode(), (ConstraintValue<A>) rhs, lhs.getOutEdge());
    } else if (lhs instanceof ConstraintValue) {
      constraints.addEdge((ConstraintValue<A>) lhs, rhs.getNode(), rhs.getInEdge());
    } else {
      throw new IllegalArgumentException(
          "Either the left-hand or the right-hand side needs to be an atomic term.");
    }
  }

  /** A variable for the solver to find a value for. */
  public final class VariableTerm implements ConstraintValue<A> {
    private VariableTerm() {}

    @Override
    public A initialize() {
      return bottom;
    }

    @Override
    public A transfer(A newValue) {
      return newValue;
    }
  }
}
