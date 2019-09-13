package edu.cornell.cs.apl.viaduct.util.dataflow;

/** Indicates that an edge in a data flow graph induces an unsatisfiable equality. */
final class UnsatisfiableEqualityException extends Exception {
  private final DataFlowEdge<?> edge;

  UnsatisfiableEqualityException(DataFlowEdge<?> edge) {
    this.edge = edge;
  }

  public DataFlowEdge<?> getEdge() {
    return edge;
  }
}
