package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.BrouwerianLattice;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.util.PartialOrder;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow.DataflowDirection;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;

/**
 * Given a set of constraints of the form {@code t1 <= t2}, finds the unique minimum solution, if it
 * exists.
 *
 * <p>A solution to a set of constraints is an assignment of values to all variables in the system.
 * A minimum solution assigns the smallest possible value to each variable, where smallest is with
 * respect to {@link PartialOrder#lessThanOrEqualTo(Object)}.
 */
public class ConstraintSystem<A extends BrouwerianLattice<A>> {
  /**
   * Maintain constraints as a graph. Each vertex corresponds to an atomic term (a variable or a
   * constant), and an edge from term {@code t1} to {@code t2} corresponds to the constraint {@code
   * f(t1) <= t2} where {@code f} is a function determined by the edge.
   */
  private final DirectedPseudograph<ConstraintValue<A>, DataFlowEdge<A>> constraints =
      new DirectedPseudograph<>(null);

  /** Least element of {@code A}. */
  private final A init;

  /**
   * Create a new constraint system.
   *
   * @param init initial value of variables {@code A}
   */
  public ConstraintSystem(A init) {
    this.init = init;
  }

  /**
   * Find a least solution to the set of constraints in the system.
   *
   * @return Mapping from variables to the smallest values that satisfy all constraints
   */
  public Map<VariableTerm<A>, A> solve() throws UnsatisfiableConstraintException {
    // Use data flow analysis to find a solution for all nodes.
    Map<ConstraintValue<A>,A> solutions =
        DataFlow.solve(init, constraints, DataflowDirection.DOWN);

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
    VariableTerm<A> term = new VariableTerm<>(id, this.init);
    this.constraints.addVertex(term);
    return term;
  }

  /** Create a fresh variable and add it to the system. */
  public VariableTerm<A> addNewVariable(String id, String label) {
    VariableTerm<A> term = new VariableTerm<>(id, label, this.init);
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
      constraints.addEdge((ConstraintValue<A>)rhs, lhs.getNode(), lhs.getInEdge());

    } else if (lhs instanceof ConstraintValue) {
      constraints.addEdge(rhs.getNode(), (ConstraintValue<A>)lhs, rhs.getOutEdge());

    } else {
      throw new IllegalArgumentException(
          "Either the left-hand or the right-hand side needs to be an atomic term.");
    }
  }

  /** output constraint system as a DOT graph. */
  public void exportDotGraph(Map<VariableTerm<A>,A> solutions, Writer writer) {
    FreshNameGenerator nameGenerator = new FreshNameGenerator();

    ComponentNameProvider<ConstraintValue<A>> vertexIdProvider =
        new ComponentNameProvider<ConstraintValue<A>>() {
          public String getName(ConstraintValue<A> val) {
            if (val instanceof VariableTerm) {
              return ((VariableTerm<A>)val).getId();

            } else if (val instanceof ConstantTerm) {
              return nameGenerator.getFreshName("const");

            } else {
              throw new Error("unknown vertex type in constraint system");
            }
          }
        };

    ComponentNameProvider<ConstraintValue<A>> vertexLabelProvider =
        new ComponentNameProvider<ConstraintValue<A>>() {
          public String getName(ConstraintValue<A> val) {
            String id =  val.getNode().toString();
            String label = "";

            if (solutions != null) {
              if (val instanceof VariableTerm) {
                VariableTerm<A> var = (VariableTerm<A>)val;
                label = solutions.get(var).toString();

              } else {
                return val.getNode().toString();
              }
            }

            return id + '\n' + label;
          }
        };

    ComponentNameProvider<DataFlowEdge<A>> edgeLabelProvider =
        new ComponentNameProvider<DataFlowEdge<A>>() {
          public String getName(DataFlowEdge<A> edge) {
            if (edge instanceof IdentityEdge) {
              return "";

            } else if (edge instanceof JoinEdge) {
              JoinEdge<A> joinEdge = (JoinEdge<A>)edge;
              A joinConstant = joinEdge.getJoinConstant();
              return String.format(" | %s", joinConstant);

            } else if (edge instanceof PseudocomplementEdge) {
              PseudocomplementEdge<A> psEdge = (PseudocomplementEdge<A>)edge;
              A constant = psEdge.getPseudocomplementedConstant();
              return String.format(" -> %s", constant);

            } else {
              throw new Error("unknown edge in constraint system");
            }
          }
        };

    DOTExporter<ConstraintValue<A>,DataFlowEdge<A>> exporter =
        new DOTExporter<>(vertexIdProvider, vertexLabelProvider, edgeLabelProvider);
    exporter.exportGraph(constraints, writer);
  }
}
