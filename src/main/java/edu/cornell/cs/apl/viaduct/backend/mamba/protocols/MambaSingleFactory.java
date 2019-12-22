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
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.SingleHostProtocolFactory;

import io.vavr.collection.Map;

/** cleartext data in a MAMBA program. */
public class MambaSingleFactory extends SingleHostProtocolFactory<ImpAstNode> {
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protoMap,
      HostTrustConfiguration hostConfig,
      HostName host)
  {
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
        if (readProto != null && readProto instanceof MambaSecret) {
          hasHostCommunication = true;
          break;
        }
      }

      for (PdgWriteEdge<ImpAstNode> writeEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> writeProto = protoMap.getOrElse(writeEdge.getTarget(), null);
        if (writeProto != null && writeProto instanceof MambaSecret) {
          hasHostCommunication = true;
          break;
        }
      }

      if (node.getAstNode() instanceof CommunicationNode) {
        hasHostCommunication = true;
      }
    }

    if (!inSecretConditional) {
      return new Single(hostConfig, host);

    } else {
      return null;
    }
  }
}
