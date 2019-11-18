package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ZKFactory;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

/** create protocol instances and estimate protocol cost for IMP programs. */
public class ImpZKProtocolSearchStrategy extends ImpProtocolSearchStrategy {
  private final SingleFactory singleFactory;
  private final ZKFactory zkFactory;
  private final CommitmentFactory commitmentFactory;

  /** constructor. */
  public ImpZKProtocolSearchStrategy(ProtocolCostEstimator<ImpAstNode> costEstimator) {
    super(costEstimator);
    this.singleFactory = new SingleFactory();
    this.zkFactory = new ZKFactory();
    this.commitmentFactory = new CommitmentFactory();
  }

  @Override
  public Set<Protocol<ImpAstNode>> createNormalProtocolInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node)
  {
    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    instances.addAll(this.singleFactory.createInstances(hostConfig, protocolMap, node));
    if (instances.size() > 0) {
      return instances;
    }

    instances.addAll(this.commitmentFactory.createInstances(hostConfig, protocolMap, node));
    if (instances.size() > 0) {
      return instances;
    }

    instances.addAll(this.zkFactory.createInstances(hostConfig, protocolMap, node));

    return instances;
  }
}
