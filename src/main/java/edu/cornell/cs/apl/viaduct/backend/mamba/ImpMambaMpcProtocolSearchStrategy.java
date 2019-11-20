package edu.cornell.cs.apl.viaduct.backend.mamba;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

/** try to put everything in MPC as much as possible. */
public class ImpMambaMpcProtocolSearchStrategy extends ImpMambaProtocolSearchStrategy {
  public ImpMambaMpcProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    super(costEstimator);
  }

  @Override
  public Set<Protocol<ImpAstNode>> createNormalProtocolInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node)
  {
    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    instances.addAll(this.mambaSecretFactory.createInstances(hostConfig, protocolMap, node));

    // because muxing secret conditionals can cause backtracking, don't prune away
    // replication protocols immediately for loop guards
    if (instances.size() > 0 && !node.isGuard()) {
      return instances;
    }
    instances.addAll(this.mambaPublicFactory.createInstances(hostConfig, protocolMap, node));

    if (instances.size() > 0) {
      return instances;
    }
    instances.addAll(this.singleFactory.createInstances(hostConfig, protocolMap, node));

    return instances;
  }
}
