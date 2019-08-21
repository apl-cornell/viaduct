package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPC;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPCFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ReplicationFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ZKFactory;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** create protocol instances and estimate protocol cost for IMP programs. */
public class ImpProtocolSearchStrategy extends ProtocolCostEstimator<ImpAstNode>
    implements ProtocolSearchStrategy<ImpAstNode> {

  private final SingleFactory singleFactory;
  private final ReplicationFactory replicationFactory;
  private final MPCFactory mpcFactory;
  private final ZKFactory zkFactory;
  private final ProtocolCostEstimator<ImpAstNode> costEstimator;

  /** constructor. */
  public ImpProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    this.singleFactory = new SingleFactory();
    this.replicationFactory = new ReplicationFactory();
    this.mpcFactory = new MPCFactory();
    this.zkFactory = new ZKFactory();
    this.costEstimator = costEstimator;
  }

  /** estimate cost for a single PDG node. */
  @Override
  public int estimateNodeCost(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException {

    return this.costEstimator.estimateNodeCost(node, protocolMap, pdg);
  }

  @Override
  public Set<Protocol<ImpAstNode>> createProtocolInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {
    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    if (node.isControlNode()) {
      instances.add(ControlProtocol.getInstance());
      return instances;

    } else {
      // special case: if compute node is an assignment, set its protocol to the protocol
      // of the storage node it is writing to, given that it is not replicated.
      Set<PdgWriteEdge<ImpAstNode>> writeEdges = node.getWriteEdges();
      int numWriteEdges = writeEdges.size();
      assert numWriteEdges <= 1;
      if (numWriteEdges == 1) {
        PdgWriteEdge<ImpAstNode> writeEdge = writeEdges.iterator().next();
        PdgNode<ImpAstNode> targetNode = writeEdge.getTarget();
        Protocol<ImpAstNode> targetProto = protocolMap.get(targetNode);

        if (targetProto instanceof Single) {
          instances.add(new Single(((Single) targetProto).getHost()));
          return instances;

        } else if (targetProto instanceof MPC) {
          instances.add(new MPC(((MPC) targetProto).getParties()));
          return instances;
        }
      }

      // general case: get instances from Single, Replication, ZK, and MPC in that order
      instances.addAll(this.singleFactory.createInstances(hostConfig, protocolMap, node));

      // prune search space by not selecting MPC unless absolutely necessary
      // ie. only use MPC when neither Single nor Replication protocols can instantiate the node
      // if (instances.size() > 0 && node.isStorageNode()) {
      if (instances.size() > 0) {
        return instances;
      }

      instances.addAll(this.replicationFactory.createInstances(hostConfig, protocolMap, node));

      if (instances.size() > 0) {
        return instances;
      }

      instances.addAll(this.mpcFactory.createInstances(hostConfig, protocolMap, node));
      // instances.addAll(this.zkFactory.createInstances(hostConfig, protocolMap, node));

      return instances;
    }
  }
}
