package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.protocol.PairProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import io.vavr.Tuple2;

import java.util.Set;

/** contains ZK information flow constraints. */
public class CommitmentFactory extends PairProtocolFactory<ImpAstNode> {
  private Protocol<ImpAstNode> createInstanceFromStorageNode(
      PdgNode<ImpAstNode> node,
      HostTrustConfiguration hostConfig,
      Tuple2<HostName, HostName> hostPair)
  {
    ImpAstNode astNode = node.getAstNode();

    if (astNode instanceof VariableDeclarationNode) {
      VariableDeclarationNode varDecl = (VariableDeclarationNode) astNode;
      return new Commitment(hostConfig, hostPair._1(), hostPair._2(), varDecl.getVariable());
    }

    return null;
  }

  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      HostTrustConfiguration hostConfig,
      Tuple2<HostName, HostName> hostPair)
  {
    if (node.isStorageNode()) {
      return createInstanceFromStorageNode(node, hostConfig, hostPair);

    } else {
      // reads and writes to a committed variable should go to its commitment process
      Set<PdgWriteEdge<ImpAstNode>> writeEdges = node.getWriteEdges();

      if (writeEdges.size() == 1) {
        PdgWriteEdge<ImpAstNode> writeEdge = (PdgWriteEdge<ImpAstNode>) writeEdges.toArray()[0];
        return createInstanceFromStorageNode(writeEdge.getTarget(), hostConfig, hostPair);
      }
    }

    return null;
  }
}
