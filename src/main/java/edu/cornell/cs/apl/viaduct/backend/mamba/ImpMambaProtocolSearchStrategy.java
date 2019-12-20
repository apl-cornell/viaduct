package edu.cornell.cs.apl.viaduct.backend.mamba;

import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublicFactory;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecretFactory;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSingleFactory;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ImpProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import io.vavr.collection.Map;
import java.util.HashSet;
import java.util.Set;

public class ImpMambaProtocolSearchStrategy extends ImpProtocolSearchStrategy {
  protected final MambaSingleFactory singleFactory;
  protected final MambaPublicFactory mambaPublicFactory;
  protected final MambaSecretFactory mambaSecretFactory;

  /** constructor. */
  public ImpMambaProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    super(costEstimator);
    this.singleFactory = new MambaSingleFactory();
    this.mambaPublicFactory = new MambaPublicFactory();
    this.mambaSecretFactory = new MambaSecretFactory();
  }

  @Override
  protected Set<Protocol<ImpAstNode>> createNormalProtocolInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {
    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

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
