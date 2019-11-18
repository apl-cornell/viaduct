package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPCFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ReplicationFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

/** create protocol instances and estimate protocol cost for IMP programs. */
public class ImpProtocolSearchStrategy extends ProtocolCostEstimator<ImpAstNode>
    implements ProtocolSearchStrategy<ImpAstNode> {

  private final SingleFactory singleFactory;
  private final ReplicationFactory replicationFactory;
  private final MPCFactory mpcFactory;
  private final ProtocolCostEstimator<ImpAstNode> costEstimator;

  /** constructor. */
  public ImpProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    this.singleFactory = new SingleFactory();
    this.replicationFactory = new ReplicationFactory();
    this.mpcFactory = new MPCFactory();
    this.costEstimator = costEstimator;
  }

  /** estimate cost for a single PDG node. */
  @Override
  public int estimateNodeCost(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException {

    ImpAstNode astNode = node.getAstNode();

    // don't attempt to estimate cost for external communication;
    // the protocols for these are fixed and should not be factored into
    // the cost of the overall protocol. also, fixing the protocols for
    // external communication breaks the invariant that all of a node's
    // dependencies will have a protocol by the time the node's protocol
    // gets selected, so this is actually necessary to prevent stuff
    // from breaking
    if (astNode instanceof ReceiveNode || astNode instanceof SendNode) {
      return 0;
    }

    // check if all inputs to the node have protocols;
    // if not, don't estimate node cost yet
    for (PdgNode<ImpAstNode> readNode : node.getReadNodes()) {
      if (!protocolMap.containsKey(readNode)) {
        return 0;
      }
    }

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
      ImpAstNode astNode = node.getAstNode();
      if (astNode instanceof DowngradeNode) {
        Set<PdgNode<ImpAstNode>> readNodes = node.getReadNodes();
        assert readNodes.size() == 1;

        PdgNode<ImpAstNode> readNode = (PdgNode<ImpAstNode>) readNodes.toArray()[0];
        instances.add(protocolMap.getOrElse(readNode, null));
        return instances;
      }

      // general case: get instances from Single, Replication, and MPC in that order
      instances.addAll(this.singleFactory.createInstances(hostConfig, protocolMap, node));

      instances.addAll(this.replicationFactory.createInstances(hostConfig, protocolMap, node));

      // prune search space by not selecting MPC unless absolutely necessary
      // ie. only use MPC when neither Single nor Replication protocols can instantiate the node
      if (instances.size() > 0 && node.isStorageNode()) {
        return instances;
      }

      instances.addAll(this.mpcFactory.createInstances(hostConfig, protocolMap, node));
      // instances.addAll(this.zkFactory.createInstances(hostConfig, protocolMap, node));

      return instances;
    }
  }
}
