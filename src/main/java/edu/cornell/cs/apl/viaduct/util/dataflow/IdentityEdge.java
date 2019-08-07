package edu.cornell.cs.apl.viaduct.util.dataflow;

import org.jgrapht.graph.DefaultEdge;

/** An edge that passes values through unmodified. */
public class IdentityEdge<A> extends DefaultEdge implements DataFlowEdge<A> {
  @Override
  public A propagate(A in) {
    return in;
  }
}
