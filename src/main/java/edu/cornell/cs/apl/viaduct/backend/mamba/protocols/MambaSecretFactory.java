package edu.cornell.cs.apl.viaduct.backend.mamba.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.CommunicationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.protocol.AllHostsProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import io.vavr.collection.Map;
import java.util.Set;

/** cleartext data in a MAMBA program. */
public class MambaSecretFactory extends AllHostsProtocolFactory<ImpAstNode> {
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protoMap,
      HostTrustConfiguration hostConfig,
      Set<HostName> hostSet) {
    boolean inSecretConditional = false;
    for (PdgControlNode<ImpAstNode> controlNode : node.getConditionalNodeStack()) {
      for (PdgReadEdge<ImpAstNode> controlReadEdge : controlNode.getReadEdges()) {
        if (protoMap.getOrElse(controlReadEdge.getSource(), null) instanceof MambaSecret) {
          inSecretConditional = true;
          break;
        }
      }
    }

    boolean hasHostCommunication = false;
    if (inSecretConditional) {
      for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> readProto = protoMap.getOrElse(readEdge.getSource(), null);
        if (readProto != null && readProto instanceof Single) {
          hasHostCommunication = true;
          break;
        }
      }

      for (PdgWriteEdge<ImpAstNode> writeEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> writeProto = protoMap.getOrElse(writeEdge.getTarget(), null);
        if (writeProto != null && writeProto instanceof Single) {
          hasHostCommunication = true;
          break;
        }
      }

      if (node.getAstNode() instanceof CommunicationNode) {
        hasHostCommunication = true;
      }
    }

    if (!node.isArrayIndex()
        && !node.isLoopGuard()
        && !(inSecretConditional && hasHostCommunication)) {
      return hostSet.size() >= 2 ? new MambaSecret(hostConfig, hostSet) : null;

    } else {
      return null;
    }
  }
}
