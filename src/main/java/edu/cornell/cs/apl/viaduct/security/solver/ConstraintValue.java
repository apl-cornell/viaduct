package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;

/** Atomic constraint terms such as constants and variables, but not expressions. */
public interface ConstraintValue<A>
    extends LeftHandTerm<A>, RightHandTerm<A>, DataFlowNode<A, UnsatisfiableConstraintException> {

  @Override
  default ConstraintValue<A> getNode() {
    return this;
  }

  @Override
  default DataFlowEdge<A> getOutEdge() {
    return new IdentityEdge<>();
  }

  @Override
  default DataFlowEdge<A> getInEdge() {
    return new IdentityEdge<>();
  }
}
