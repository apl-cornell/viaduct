package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ControlNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
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
    /** except external communication and storage of data will be executed in their
     * respective hosts. */
    Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protoMap = HashMap.empty();
    Map<Variable, HostName> hostVariables = HashMap.empty();
    for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
      ImpAstNode astNode = node.getAstNode();
      if (astNode instanceof ReceiveNode) {
        ReceiveNode recvNode = (ReceiveNode) astNode;
        HostName host = recvNode.getSender().toHostName();
        protoMap = protoMap.put(node, new Single(hostConfig, host));
        hostVariables = hostVariables.put(recvNode.getVariable(), host);

      } else if (astNode instanceof SendNode) {
        SendNode sendNode = (SendNode) astNode;
        protoMap = protoMap.put(node, new Single(hostConfig, sendNode.getRecipient().toHostName()));
      }
    }

    for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
      if (!protoMap.containsKey(node)) {
        ImpAstNode astNode = node.getAstNode();
        if (astNode instanceof DeclarationNode) {
          DeclarationNode declNode = (DeclarationNode) astNode;
          Variable var = declNode.getVariable();
          if (hostVariables.containsKey(var)) {
            HostName host = hostVariables.getOrElse(var, null);
            protoMap = protoMap.put(node, new Single(hostConfig, host));
          }

        } else if (astNode instanceof ControlNode) {
          protoMap = protoMap.put(node, ControlProtocol.getInstance());
        }
      }
    }

    return protoMap;
  }
}
