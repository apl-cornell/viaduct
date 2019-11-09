package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchSelection;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

public final class ImpProtocolSearchSelection extends ProtocolSearchSelection<ImpAstNode> {
  public ImpProtocolSearchSelection(
      boolean enableProfiling, ProtocolSearchStrategy<ImpAstNode> strategy)
  {
    super(enableProfiling, strategy);
  }

  @Override
  protected Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> getInitialProtocolMap(
      HostTrustConfiguration hostConfig, ProgramDependencyGraph<ImpAstNode> pdg)
  {
    Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> initMap = HashMap.empty();
    for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
      ImpAstNode astNode = node.getAstNode();
      if (astNode instanceof ReceiveNode) {
        ReceiveNode recvNode = (ReceiveNode) astNode;
        initMap = initMap.put(node, new Single(hostConfig, recvNode.getSender().toHostName()));

      } else if (astNode instanceof SendNode) {
        SendNode sendNode = (SendNode) astNode;
        initMap = initMap.put(node, new Single(hostConfig, sendNode.getRecipient().toHostName()));
      }
    }

    return initMap;
  }
}
