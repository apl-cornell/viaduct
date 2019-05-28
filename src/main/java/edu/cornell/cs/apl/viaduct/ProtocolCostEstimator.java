package edu.cornell.cs.apl.viaduct;

import java.util.Map;
import java.util.Set;

/** estimates cost for a mapping from PDG nodes to protocols. */
public abstract class ProtocolCostEstimator<T extends AstNode> {
  protected ProtocolCostEstimator() {}

  public abstract Set<Protocol<T>> getProtocols();

  protected abstract int estimateNodeCost(
      Protocol<T> protocol, PdgNode<T> node, ProgramDependencyGraph<T> pdg)
      throws UnknownProtocolException;

  /** estimate the cost of the pdg given a protocol mapping. */
  public int estimatePdgCost(
      Map<PdgNode<T>, Protocol<T>> protocolMap, ProgramDependencyGraph<T> pdg)
      throws UnknownProtocolException {

    // next, tally all the costs for each node
    int cost = 0;
    for (Map.Entry<PdgNode<T>, Protocol<T>> kv : protocolMap.entrySet()) {
      cost += estimateNodeCost(kv.getValue(), kv.getKey(), pdg);
    }

    return cost;
  }
}
