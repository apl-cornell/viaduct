package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.PartialOrder;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;

/**
 * Given a set of constraints of the form {@code t1 <= t2}, finds the unique maximum solution, if it
 * exists.
 *
 * <p>A solution to a set of constraints is an assignment of values to all variables in the system.
 * A maximum solution assigns the greatest possible value to each variable, where greatest is with
 * respect to {@link PartialOrder#lessThanOrEqualTo(Object)}.
 */
public class ConstraintSystem<A extends HeytingAlgebra<A>> {
  /**
   * Maintain constraints as a graph. Each vertex corresponds to an atomic term (a variable or a
   * constant), and an edge from term {@code t1} to {@code t2} corresponds to the constraint {@code
   * f(t1) <= t2} where {@code f} is a function determined by the edge.
   */
  private final DirectedPseudograph<ConstraintValue<A>, DataFlowEdge<A>> constraints =
      new DirectedPseudograph<>(null);

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
   * Find a greatest solution to the set of constraints in the system.
   *
   * @return Mapping from variables to the greatest values that satisfy all constraints
   */
  public Map<VariableTerm<A>, A> solve() throws UnsatisfiableConstraintException {
    // Use data flow analysis to find a solution for all nodes.
    Map<ConstraintValue<A>, A> solutions = DataFlow.solve(top, constraints);

    // Only return solutions for nodes that correspond to variables.
    final Map<VariableTerm<A>, A> variableSolutions = new HashMap<>();
    for (Map.Entry<ConstraintValue<A>, A> entry : solutions.entrySet()) {
      if (entry.getKey() instanceof VariableTerm) {
        variableSolutions.put(((VariableTerm<A>) entry.getKey()), entry.getValue());
      }
    }

    return variableSolutions;
  }

  /** Create a fresh variable and add it to the system. */
  public VariableTerm<A> addNewVariable(String id) {
    VariableTerm<A> term = new VariableTerm<>(id, this.top);
    this.constraints.addVertex(term);
    return term;
  }

  /** Create a fresh variable and add it to the system. */
  public VariableTerm<A> addNewVariable(String id, String label) {
    VariableTerm<A> term = new VariableTerm<>(id, label, this.top);
    this.constraints.addVertex(term);
    return term;
  }

  /** create a new constant term and add it to the system. */
  public ConstantTerm<A> addNewConstant(A constant) {
    ConstantTerm<A> constantTerm = ConstantTerm.create(constant);
    this.constraints.addVertex(constantTerm);
    return constantTerm;
  }

  /** Add the constraint {@code lhs <= rhs} to the system. */
  public void addLessThanOrEqualToConstraint(LeftHandTerm<A> lhs, RightHandTerm<A> rhs) {
    if (rhs instanceof ConstraintValue) {
      constraints.addEdge((ConstraintValue<A>) rhs, lhs.getNode(), lhs.getInEdge());

    } else if (lhs instanceof ConstraintValue) {
      constraints.addEdge(rhs.getNode(), (ConstraintValue<A>) lhs, rhs.getOutEdge());

    } else {
      throw new IllegalArgumentException(
          "Either the left-hand or the right-hand side needs to be an atomic term.");
    }
  }

  // TODO: clean this up
  /** output constraint system as a DOT graph. */
  public void exportDotGraph(Map<VariableTerm<A>, A> solutions, Writer writer) {
    ComponentNameProvider<ConstraintValue<A>> vertexIdProvider =
        new ComponentNameProvider<ConstraintValue<A>>() {
          @Override
          public String getName(ConstraintValue<A> val) {
            return val.getId();
          }
        };

    ComponentNameProvider<ConstraintValue<A>> vertexLabelProvider =
        new ComponentNameProvider<ConstraintValue<A>>() {
          @Override
          public String getName(ConstraintValue<A> val) {
            String id = "";
            String label = "";

            if (val instanceof VariableTerm) {
              id = val.getNode().toString();
              VariableTerm<A> var = (VariableTerm<A>) val;
              if (solutions != null && solutions.containsKey(var)) {
                label = solutions.get(var).toString();
              }

            } else {
              id = val.getId();
              label = val.getNode().toString();
            }

            return id + '\n' + label;
          }
        };

    ComponentNameProvider<DataFlowEdge<A>> edgeLabelProvider =
        new ComponentNameProvider<DataFlowEdge<A>>() {
          @Override
          public String getName(DataFlowEdge<A> edge) {
            if (edge instanceof IdentityEdge) {
              return "";

            } else if (edge instanceof JoinEdge) {
              JoinEdge<A> joinEdge = (JoinEdge<A>) edge;
              A joinConstant = joinEdge.getJoinConstant();
              return String.format(" | %s", joinConstant);

            } else if (edge instanceof PseudocomplementEdge) {
              PseudocomplementEdge<A> psEdge = (PseudocomplementEdge<A>) edge;
              A constant = psEdge.getPseudocomplementedConstant();
              return String.format("%s ---> _", constant);

            } else {
              throw new Error("unknown edge in constraint system");
            }
          }
        };

    DOTExporter<ConstraintValue<A>, DataFlowEdge<A>> exporter =
        new DOTExporter<>(vertexIdProvider, vertexLabelProvider, edgeLabelProvider);
    exporter.exportGraph(constraints, writer);
  }
}
