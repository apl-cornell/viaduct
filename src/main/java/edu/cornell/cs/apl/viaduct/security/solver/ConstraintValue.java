package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode;
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge;

/** Atomic constraint terms such as constants and variables, but not expressions. */
public abstract class ConstraintValue<A> implements
    LeftHandTerm<A>, RightHandTerm<A>,
    DataFlowNode<A, UnsatisfiableConstraintError>
{

  protected String id;

  protected ConstraintValue(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  @Override
  public ConstraintValue<A> getNode() {
    return this;
  }

  @Override
  public DataFlowEdge<A> getOutEdge() {
    return new IdentityEdge<>();
  }

  @Override
  public DataFlowEdge<A> getInEdge() {
    return new IdentityEdge<>();
  }
}
