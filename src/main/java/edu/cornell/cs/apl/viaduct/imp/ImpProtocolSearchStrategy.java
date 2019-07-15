package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPC;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPCFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.Replication;
import edu.cornell.cs.apl.viaduct.imp.protocols.ReplicationFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ZK;
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

  private final SingleFactory singleFactory = new SingleFactory();
  private final ReplicationFactory replicationFactory = new ReplicationFactory();
  private final MPCFactory mpcFactory = new MPCFactory();
  private final ZKFactory zkFactory = new ZKFactory();

  /** estimate cost for a single PDG node. */
  @Override
  public int estimateNodeCost(
      Protocol<ImpAstNode> protocol,
      PdgNode<ImpAstNode> node,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException {
    // ImpAstNode astNode = node.getAstNode();

    if (protocol instanceof Single) {
      // return 1 * astNode.accept(nodeSizer);
      return 1;

    } else if (protocol instanceof Replication) {
      // return 5 * astNode.accept(nodeSizer);
      Replication replProto = (Replication) protocol;
      return replProto.getRealReplicas().size() + (2 * replProto.getHashReplicas().size());

    } else if (protocol instanceof ZK) {
      // return 10 * astNode.accept(nodeSizer);
      return 10;

    } else if (protocol instanceof MPC) {
      // return 100 * astNode.accept(nodeSizer);
      return 100;

    } else if (protocol instanceof ControlProtocol) {
      return 0;

    } else {
      throw new UnknownProtocolException(protocol);
    }
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
      instances.addAll(this.replicationFactory.createInstances(hostConfig, protocolMap, node));

      // prune search space by not selecting MPC unless absolutely necessary
      // ie. only use MPC when neither Single nor Replication protocols can instantiate the node
      if (instances.size() > 0) {
        return instances;
      }

      instances.addAll(this.mpcFactory.createInstances(hostConfig, protocolMap, node));
      // instances.addAll(this.zkFactory.createInstances(hostConfig, protocolMap, node));

      return instances;
    }
  }
}
