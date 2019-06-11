package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** contains Single protocol information flow constraints. */
public class SingleFactory implements ProtocolFactory<ImpAstNode> {
  @Override
  public Set<Protocol<ImpAstNode>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {

    HashSet<Protocol<ImpAstNode>> instances = new HashSet<>();
    if (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode()) {
      for (Host h : hostConfig.hosts()) {
        Label hLabel = hostConfig.getTrust(h);
        Label nInLabel = node.getInLabel();

        if (nInLabel.confidentiality().flowsTo(hLabel.confidentiality())
            && hLabel.integrity().flowsTo(nInLabel.integrity())) {
          instances.add(new Single(h));
        }
      }
    }
    return instances;
  }
}
