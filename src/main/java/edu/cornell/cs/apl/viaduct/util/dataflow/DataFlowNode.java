package edu.cornell.cs.apl.viaduct.util.dataflow;

/** Nodes of a data flow graph. */
public interface DataFlowNode<A, T extends Throwable> {
  /**
   * Initial guess for the value of this node. This is required to be a lower bound on the actual
   * solution.
   */
  A initialize();

  /**
   * Given the join of the values from the incoming edges, compute the outgoing value. The final
   * outgoing value for this node will be returned as the solution for this node.
   *
   * <p>This function is allowed to throw an exception, in which case data flow analysis aborts with
   * the same exception.
   */
  A transfer(A in) throws T;
}
