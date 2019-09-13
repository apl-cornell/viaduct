package edu.cornell.cs.apl.viaduct.util.dataflow;

import java.util.Optional;

/** Nodes in a data flow graph. */
public interface DataFlowNode<A> {
  /**
   * Initial guess for the value of this node. This is required to be an upper bound on the actual
   * solution.
   */
  A initialize();

  /**
   * This function will be called for each incoming edge, and allows the node to modify incoming
   * values. The overall outgoing value of this node will be the meet of all values produced by this
   * function.
   *
   * <p>This function is allowed to return an empty optional, in which case data flow analysis is
   * aborted, and the edge that caused the empty value is reported as the cause of error.
   */
  Optional<A> transfer(A in);
}
