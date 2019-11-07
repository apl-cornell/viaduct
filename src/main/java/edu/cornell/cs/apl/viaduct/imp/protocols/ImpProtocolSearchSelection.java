package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchSelection;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;

import java.util.HashMap;

public final class ImpProtocolSearchSelection extends ProtocolSearchSelection<ImpAstNode> {
  public ImpProtocolSearchSelection(
      boolean enableProfiling, ProtocolSearchStrategy<ImpAstNode> strategy)
  {
    super(enableProfiling, strategy);
  }

  @Override
  protected HashMap<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> getInitialProtocolMap(
      HostTrustConfiguration hostConfig, ProgramDependencyGraph<ImpAstNode> pdg)
  {
    HashMap<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> initMap = new HashMap<>();
    for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
      ImpAstNode astNode = node.getAstNode();
      if (astNode instanceof ReceiveNode) {
        ReceiveNode recvNode = (ReceiveNode) astNode;
        initMap.put(node, new Single(hostConfig, recvNode.getSender().toHostName()));

      } else if (astNode instanceof SendNode) {
        SendNode sendNode = (SendNode) astNode;
        initMap.put(node, new Single(hostConfig, sendNode.getRecipient().toHostName()));
      }
    }

    return initMap;
  }
}
