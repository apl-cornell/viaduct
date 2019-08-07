package edu.cornell.cs.apl.viaduct.util.dataflow;

/**
 * Edges of a data flow graph. These edges are allowed to modify the values passing through them.
 */
public interface DataFlowEdge<A> {
  /**
   * Apply this function to the value incoming from the source node before passing it onto the
   * destination node.
   */
  A propagate(A in);
}
