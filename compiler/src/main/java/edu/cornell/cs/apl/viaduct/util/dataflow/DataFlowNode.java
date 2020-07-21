package edu.cornell.cs.apl.viaduct.util.dataflow;

/** Nodes in a data flow graph. */
public interface DataFlowNode<A> {
  /**
   * Compute the output value for this node given the meet of all values from incoming edges. The
   * incoming values might only be upper bounds rather than being exact, in which case this function
   * should return an upper bound. When the incoming values are exact, this function should also
   * return an exact answer.
   */
  A transfer(A in);
}
