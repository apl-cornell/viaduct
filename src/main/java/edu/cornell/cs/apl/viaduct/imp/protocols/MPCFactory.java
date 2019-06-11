package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** contains MPC information flow constraints. */
public class MPCFactory implements ProtocolFactory<ImpAstNode> {
  @Override
  public Set<Protocol<ImpAstNode>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {

    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    Set<PdgNode<ImpAstNode>> inNodes = new HashSet<>();
    for (PdgInfoEdge<ImpAstNode> edge : node.getReadEdges()) {
      PdgNode<ImpAstNode> source = edge.getSource();
      if (!source.isControlNode()) {
        inNodes.add(source);
      }
    }

    Label nOutLabel = node.getOutLabel();
    boolean noInputFlow = true;
    for (PdgNode<ImpAstNode> inNode : inNodes) {
      if (inNode.getOutLabel().confidentiality().flowsTo(nOutLabel.confidentiality())) {
        noInputFlow = false;
        break;
      }
    }

    if (!node.isDeclassifyNode() || !noInputFlow) {
      return instances;
    }

    Set<Host> inHosts = new HashSet<>();
    for (PdgNode<ImpAstNode> inNode : inNodes) {
      Protocol<ImpAstNode> inProto = protocolMap.get(inNode);
      if (inProto instanceof Single) {
        inHosts.addAll(inProto.getHosts());

      } else {
        return instances;
      }
    }

    if (inHosts.size() <= 1) {
      return instances;
    }

    Label hsLabel = Label.top();
    for (Host h : inHosts) {
      hsLabel = hsLabel.meet(hostConfig.getTrust(h));
    }

    if (nOutLabel.confidentiality().flowsTo(hsLabel.confidentiality())) {
      instances.add(new MPC(inHosts));
    }
    /*
    PowersetIterator<Host> hostPowerset = new PowersetIterator<>(hostConfig.hostSet());
    for (Set<Host> hostSet : hostPowerset) {
      if (hostSet.size() > 1) {
        Label hsLabel = Label.top();
        for (Host h : hostSet) {
          hsLabel = hsLabel.meet(hostConfig.getTrust(h));
        }

        if (nOutLabel.confidentiality().flowsTo(hsLabel.confidentiality())) {
          instances.add(new MPC(hostSet));
        }
      }
    }
    */

    return instances;
  }
}
