package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;

import java.util.HashSet;
import java.util.Set;

/** cost estimator for IMP. */
public final class ImpProtocolCostEstimator extends ProtocolCostEstimator<ImpAstNode> {
  private static Set<Protocol<ImpAstNode>> protocols = new HashSet<Protocol<ImpAstNode>>();

  {
    protocols.add(ImpProtocols.Single.getInstance());
    protocols.add(ImpProtocols.Replication.getInstance());
    protocols.add(ImpProtocols.ZK.getInstance());
    protocols.add(ImpProtocols.MPC.getInstance());
  }

  public Set<Protocol<ImpAstNode>> getProtocols() {
    return protocols;
  }

  /** estimate cost for a single PDG node. */
  public int estimateNodeCost(
      Protocol<ImpAstNode> protocol, PdgNode<ImpAstNode> node,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException
  {
    if (protocol instanceof ImpProtocols.Single) {
      return 1;
    } else if (protocol instanceof ImpProtocols.Replication) {
      return 5;
    } else if (protocol instanceof ImpProtocols.ZK) {
      return 10;
    } else if (protocol instanceof ImpProtocols.MPC) {
      return 100;
    } else {
      throw new UnknownProtocolException(protocol);
    }
  }
}
