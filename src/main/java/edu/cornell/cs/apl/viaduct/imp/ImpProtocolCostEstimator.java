package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.SizeVisitor;
import java.util.HashSet;
import java.util.Set;

/** cost estimator for IMP. */
public final class ImpProtocolCostEstimator extends ProtocolCostEstimator<ImpAstNode> {
  private static final Set<Protocol<ImpAstNode>> protocols = new HashSet<>();
  private static SizeVisitor nodeSizer = new SizeVisitor();

  {
    protocols.add(ImpProtocols.Single.getRepresentative());
    /*
    protocols.add(ImpProtocols.Replication.getRepresentative());
    protocols.add(ImpProtocols.ZK.getRepresentative());
    */
    protocols.add(ImpProtocols.MPC.getRepresentative());
  }

  public Set<Protocol<ImpAstNode>> getProtocols() {
    return protocols;
  }

  /** estimate cost for a single PDG node. */
  public int estimateNodeCost(
      Protocol<ImpAstNode> protocol,
      PdgNode<ImpAstNode> node,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException {
    // ImpAstNode astNode = node.getAstNode();

    if (protocol instanceof ImpProtocols.Single) {
      // return 1 * astNode.accept(nodeSizer);
      return 1;

    } else if (protocol instanceof ImpProtocols.Replication) {
      // return 5 * astNode.accept(nodeSizer);
      ImpProtocols.Replication replProto = (ImpProtocols.Replication) protocol;
      return replProto.getRealReplicas().size() + (2 * replProto.getHashReplicas().size());

    } else if (protocol instanceof ImpProtocols.ZK) {
      // return 10 * astNode.accept(nodeSizer);
      return 10;

    } else if (protocol instanceof ImpProtocols.MPC) {
      // return 100 * astNode.accept(nodeSizer);
      return 100;

    } else {
      throw new UnknownProtocolException(protocol);
    }
  }
}
