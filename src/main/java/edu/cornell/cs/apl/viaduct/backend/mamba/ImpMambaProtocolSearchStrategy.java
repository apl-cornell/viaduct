package edu.cornell.cs.apl.viaduct.backend.mamba;

import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublicFactory;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecretFactory;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImpMambaProtocolSearchStrategy extends ProtocolCostEstimator<ImpAstNode>
    implements ProtocolSearchStrategy<ImpAstNode>
{
  private final ProtocolCostEstimator<ImpAstNode> costEstimator;

  private final SingleFactory singleFactory;
  private final MambaPublicFactory mambaPublicFactory;
  private final MambaSecretFactory mambaSecretFactory;

  /** constructor. */
  public ImpMambaProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    this.singleFactory = new SingleFactory();
    this.mambaPublicFactory = new MambaPublicFactory();
    this.mambaSecretFactory = new MambaSecretFactory();
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
      // general case: get instances from Single, Replication, ZK, and MPC in that order
      instances.addAll(this.singleFactory.createInstances(hostConfig, protocolMap, node));

      // prune search space by not selecting MPC unless absolutely necessary
      // ie. only use MPC when neither Single nor Replication protocols can instantiate the node
      // if (instances.size() > 0 && node.isStorageNode()) {
      if (instances.size() > 0) {
        return instances;
      }

      instances.addAll(this.mambaPublicFactory.createInstances(hostConfig, protocolMap, node));

      // prune search space by not selecting MPC unless absolutely necessary
      // ie. only use MPC when neither Single nor Replication protocols can instantiate the node
      if (instances.size() > 0) {
        return instances;
      }

      instances.addAll(this.mambaSecretFactory.createInstances(hostConfig, protocolMap, node));

      return instances;
    }
  }
}
