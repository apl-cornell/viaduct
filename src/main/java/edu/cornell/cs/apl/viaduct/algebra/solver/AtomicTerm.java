package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;
import java.util.Map;

/**
 * Constraint terms that are fully evaluated. For example, constants and variables, but not
 * expressions.
 */
public abstract class AtomicTerm<A extends HeytingAlgebra<A>>
    implements LeftHandTerm<A>, RightHandTerm<A>, DataFlowNode<A> {
  /** Returns the value of this term given an assignment of values to every variable in the term. */
  public abstract A getValue(Map<VariableTerm<A>, A> solution);

  /** Return a term that represents the meet of {@code this} with a constant. */
  public abstract LeftHandTerm<A> meet(A that);

  /** Return a term that represents the join of {@code this} with a constant. */
  public abstract RightHandTerm<A> join(A that);

  @Override
  public final AtomicTerm<A> getNode() {
    return this;
  }

  @Override
  public final DataFlowEdge<A> getOutEdge() {
    return new IdentityEdge<>();
  }

  @Override
  public final DataFlowEdge<A> getInEdge() {
    return new IdentityEdge<>();
  }
}
