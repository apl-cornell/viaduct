package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

/** contains ZK information flow constraints. */
public class ZKFactory implements ProtocolFactory<ImpAstNode> {
  @Override
  public Set<Protocol<ImpAstNode>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {

    /*

    if (hostConfig.size() < 2) {
      return new HashSet<>();
    }

    // assume for now that there are only two hosts
    // generalize this later
    HostName[] hostPair = new HostName[2];
    // TODO: WTF does this do?
    // hostConfig.toArray(hostPair);
    int i = 0;
    for (HostName host : hostConfig.hosts()) {
      hostPair[i] = host;
      i++;
      if (i == hostPair.length) {
        break;
      }
    }

    Set<PdgNode<ImpAstNode>> inNodes = new HashSet<>();
    for (PdgInfoEdge<ImpAstNode> edge : node.getInInfoEdges()) {
      PdgNode<ImpAstNode> source = edge.getSource();
      if (!source.isControlNode()) {
        inNodes.add(source);
      }
    }
    HostName hostA = hostPair[0];
    HostName hostB = hostPair[1];
    Label nInLabel = node.getToLabel();
    Label nOutLabel = node.getOutLabel();
    Label aLabel = hostConfig.getTrust(hostA);
    Label bLabel = hostConfig.getTrust(hostB);

    // prover: A, verifier: B
    if (inNodes.size() == 1 && node.isDeclassifyNode()) {
      PdgNode<ImpAstNode> inNode = inNodes.iterator().next();
      Protocol<ImpAstNode> inProto = protocolMap.get(inNode);
      if (inProto instanceof Replication) {
        Replication inReplProto = (Replication) inProto;

        if (nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
            && !nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
            && nOutLabel.confidentiality().flowsTo(bLabel.confidentiality())
            // && bLabel.integrity().flowsTo(nOutLabel.integrity())
            && inReplProto.getRealReplicas().contains(hostA)
            && inReplProto.getHashReplicas().contains(hostB)) {

          instances.add(new ZK(hostA, hostB));
        }

        if (nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
            && !nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
            && nOutLabel.confidentiality().flowsTo(aLabel.confidentiality())
            // && aLabel.integrity().flowsTo(nOutLabel.integrity())
            && inReplProto.getRealReplicas().contains(hostB)
            && inReplProto.getHashReplicas().contains(hostA)) {

          instances.add(new ZK(hostB, hostA));
        }
      }
    }

    return instances;
    */

    return new HashSet<>();
  }
}
