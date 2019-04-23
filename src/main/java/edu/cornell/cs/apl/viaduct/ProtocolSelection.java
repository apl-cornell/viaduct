package edu.cornell.cs.apl.viaduct;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class ProtocolSelection<T extends AstNode> {
  ProtocolCostEstimator<T> costEstimator;

  private class ProtocolMapComparator implements Comparator<Map<PdgNode<T>,Protocol<T>>> {
    private ProgramDependencyGraph<T> pdg;

    ProtocolMapComparator(ProgramDependencyGraph<T> pdg) {
      this.pdg = pdg;
    }

    public int compare(
        Map<PdgNode<T>,Protocol<T>> protoMap1,
        Map<PdgNode<T>,Protocol<T>> protoMap2) {

      int cost1 = costEstimator.estimatePdgCost(protoMap1, this.pdg);
      int cost2 = costEstimator.estimatePdgCost(protoMap2, this.pdg);
      return cost2 - cost1;
    }
  }

  public ProtocolSelection(ProtocolCostEstimator<T> estimator) {
    this.costEstimator = estimator;
  }

  /** return a mapping from PDG nodes to protocols.
   *  this uses A* search to find the cheapest protocol selection
   */
  public Map<PdgNode<T>,Protocol<T>> selectProtocols(ProgramDependencyGraph<T> pdg) {
    /*
    Set<PdgNode<T>> nodes = pdg.getNodes();
    ProtocolMapComparator comparator = new ProtocolMapComparator(pdg);

    Map<PdgNode<T>,Protocol<T>> initMap = new HashMap<>();
    PriorityQueue<Map<PdgNode<T>,Protocol<T>>> openSet =
      new PriorityQueue<>(pdg.getNodes().size(), comparator);
    openSet.add(initMap);
    */
    return null;
  }
}
