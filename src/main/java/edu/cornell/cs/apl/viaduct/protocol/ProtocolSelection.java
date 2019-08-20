package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class ProtocolSelection<T extends AstNode> {
  private final ProtocolSearchStrategy<T> strategy;
  private final boolean enableProfiling;

  public ProtocolSelection(
        boolean enableProfiling, ProtocolSearchStrategy<T> strategy)
  {
    this.enableProfiling = enableProfiling;
    this.strategy = strategy;
  }

  /**
   * return a mapping from PDG nodes to protocols. this uses A* search to find the cheapest protocol
   * selection
   */
  public Map<PdgNode<T>, Protocol<T>> selectProtocols(
      HostTrustConfiguration hostConfig, ProgramDependencyGraph<T> pdg) {

    // the sequence in which PDG nodes will have a protocol selected.
    // this allows massive pruning of the search space,
    // as we will only visit maps that follow this selection order
    // obeys toposort according to PDG
    List<PdgNode<T>> nodes = pdg.getOrderedNodes();

    // create open and closed sets
    PriorityQueue<ProtocolMapNode<T>> openSet = new PriorityQueue<>(nodes.size());
    HashSet<ProtocolMapNode<T>> closedSet = new HashSet<>();

    // start node is empty map
    HashMap<PdgNode<T>, Protocol<T>> initMap = new HashMap<>();
    openSet.add(new ProtocolMapNode<T>(initMap, 0));

    ProtocolSelectionProfiler<T> profiler =
        new ProtocolSelectionProfiler<>(this.enableProfiling, nodes.size(), 100, 100);

    // explore nodes in open set until we find a goal node
    ProtocolMapNode<T> lastAddedNode = null;
    while (!openSet.isEmpty()) {
      profiler.probe(lastAddedNode, openSet, closedSet);

      ProtocolMapNode<T> currMapNode = openSet.remove();
      HashMap<PdgNode<T>, Protocol<T>> currMap = currMapNode.getProtocolMap();
      Set<PdgNode<T>> mappedNodes = currMap.keySet();

      // check if the current map is a goal node
      // (i.e. it has a mapping for all nodes in the PDG)
      if (mappedNodes.size() == nodes.size()) {
        profiler.exitProfile();
        return currMap;
      }

      closedSet.add(currMapNode);

      // get the next node to select a protocol for,
      // according to the selection order
      PdgNode<T> nextNode = null;
      for (PdgNode<T> node : nodes) {
        if (!mappedNodes.contains(node)) {
          nextNode = node;
          break;
        }
      }
      assert nextNode != null : "nextNode is null";
      // after this point, nextNode cannot be null!
      // otherwise, that means that all PDG nodes have been mapped
      // -- but then that means we found a goal, so we should already
      // have returned

      // visit neighbors from edges, where edge set is set of protocol instantiations
      // for each protocol, generate a set of possible instantiated protocols
      // each instantiated protocol represents an edge from the current map
      // to a new map with one new mapping from the PDG node to the instantiated protocol
      Set<Protocol<T>> protoInstances =
          this.strategy.createProtocolInstances(hostConfig, currMap, nextNode);
      for (Protocol<T> protoInstance : protoInstances) {
        profiler.recordProtocol(protoInstance);

        // instantiate neighbor
        @SuppressWarnings("unchecked")
        // TODO: use functional data structures
        HashMap<PdgNode<T>, Protocol<T>> newMap =
            (HashMap<PdgNode<T>, Protocol<T>>) currMap.clone();
        newMap.put(nextNode, protoInstance);
        int newMapCost = this.strategy.estimatePdgCost(newMap, pdg);
        ProtocolMapNode<T> newMapNode = new ProtocolMapNode<>(newMap, newMapCost);

        if (!closedSet.contains(newMapNode) && !openSet.contains(newMapNode)) {
          openSet.add(newMapNode);
          lastAddedNode = newMapNode;
        }
      }
    }

    profiler.exitProfile();

    // no mapping found. should be impossible, unless available protocols + host config are bad!
    return lastAddedNode != null ? lastAddedNode.getProtocolMap() : null;
  }

  /** represents a node in the search space. */
  private static class ProtocolMapNode<U extends AstNode>
      implements Comparable<ProtocolMapNode<U>> {
    final HashMap<PdgNode<U>, Protocol<U>> protocolMap;
    final int cost;

    ProtocolMapNode(HashMap<PdgNode<U>, Protocol<U>> pmap, int cost) {
      this.protocolMap = pmap;
      this.cost = cost;
    }

    HashMap<PdgNode<U>, Protocol<U>> getProtocolMap() {
      return this.protocolMap;
    }

    @Override
    public int compareTo(ProtocolMapNode<U> other) {
      return this.cost - other.cost;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof ProtocolMapNode<?>) {
        @SuppressWarnings("unchecked")
        ProtocolMapNode<U> onode = (ProtocolMapNode<U>) o;
        return this.protocolMap.equals(onode.protocolMap);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.protocolMap.hashCode();
    }

    @Override
    public String toString() {
      StringBuffer str = new StringBuffer();
      for (Map.Entry<PdgNode<U>, Protocol<U>> kv : protocolMap.entrySet()) {
        str.append(String.format("%s => %s%n", kv.getKey().toString(), kv.getValue().toString()));
      }
      return str.toString();
    }
  }

  /** profiler for protocol search. */
  private static class ProtocolSelectionProfiler<U extends AstNode> {
    private final int interval; // how much to increase the threshold
    private final Map<Integer,Integer> openSetSizeHistogram;
    private final Map<String, Integer> protocolHistogram;
    private final int bucketSize; // bucket size for the histogram
    private final int numNodes;
    private final boolean enabled;
    private int threshold; // how big the open set must be to profile

    public ProtocolSelectionProfiler(boolean enabled, int numNodes, int i, int bucketSize) {
      this.interval = i;
      this.threshold = i;
      this.openSetSizeHistogram = new HashMap<>();
      this.protocolHistogram = new HashMap<>();
      this.bucketSize = bucketSize;
      this.numNodes = numNodes;
      this.enabled = enabled;
    }

    public void probe(
        ProtocolMapNode<U> lastAddedNode,
        PriorityQueue<ProtocolMapNode<U>> openSet,
        Set<ProtocolMapNode<U>> closedSet) {

      if (this.enabled) {
        int openSetSize = openSet.size();
        int bucket = openSetSize / this.bucketSize;
        int bucketCount = 0;

        if (this.openSetSizeHistogram.containsKey(bucket)) {
          bucketCount = this.openSetSizeHistogram.get(bucket);
        }

        this.openSetSizeHistogram.put(bucket, bucketCount + 1);

        if (openSetSize >= this.threshold) {
          System.out.println("PROBE START");
          System.out.println(String.format("size of open set: %d", openSetSize));
          System.out.println(
              String.format("completion of last added node: %d out of %d protocols selected",
                lastAddedNode.getProtocolMap().size(), this.numNodes));

          for (Map.Entry<String,Integer> kv : this.protocolHistogram.entrySet()) {
            System.out.println(String.format("protocol %s: %d", kv.getKey(), kv.getValue()));
          }
          this.threshold += this.interval;

          System.out.println("PROBE END");
        }
      }
    }

    public void recordProtocol(Protocol<U> protocol) {
      String id = protocol.getId();

      if (this.protocolHistogram.containsKey(id)) {
        Integer prev = this.protocolHistogram.get(id);
        this.protocolHistogram.put(id, prev + 1);

      } else {
        this.protocolHistogram.put(id, 1);

      }
    }

    public void exitProfile() {
      if (this.enabled) {
        System.out.println("EXIT PROFILE FOR PROTOCOL SELECTION");
        for (Map.Entry<Integer,Integer> kv : this.openSetSizeHistogram.entrySet()) {
          int bucket = kv.getKey();
          int bucketCount = kv.getValue();
          System.out.println(
              String.format("open set size %d - %d: %d",
                  this.bucketSize * bucket,
                  (this.bucketSize * (bucket + 1)) - 1,
                  bucketCount));
        }
      }
    }
  }
}
