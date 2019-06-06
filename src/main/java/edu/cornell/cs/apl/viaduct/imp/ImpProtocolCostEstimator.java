package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPC;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPCFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.Replication;
import edu.cornell.cs.apl.viaduct.imp.protocols.ReplicationFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.imp.protocols.SingleFactory;
import edu.cornell.cs.apl.viaduct.imp.protocols.ZK;
import edu.cornell.cs.apl.viaduct.imp.visitors.SizeVisitor;
import java.util.HashSet;
import java.util.Set;

/** cost estimator for IMP. */
public final class ImpProtocolCostEstimator extends ProtocolCostEstimator<ImpAstNode> {
  private static final Set<ProtocolFactory<ImpAstNode>> protocolFactories = new HashSet<>();
  private static SizeVisitor nodeSizer = new SizeVisitor();

  static {
    protocolFactories.add(new SingleFactory());
    protocolFactories.add(new ReplicationFactory());
    /*
    protocols.add(ImpProtocols.ZK.getRepresentative());
    */
    protocolFactories.add(new MPCFactory());
  }

  @Override
  public Set<ProtocolFactory<ImpAstNode>> getProtocolFactories() {
    return protocolFactories;
  }

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

    } else {
      throw new UnknownProtocolException(protocol);
    }
  }
}
