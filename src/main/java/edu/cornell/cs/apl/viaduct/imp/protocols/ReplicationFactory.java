package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PowersetIterator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReplicationFactory implements ProtocolFactory<ImpAstNode> {
  @Override
  public Set<Protocol<ImpAstNode>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {

    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    Label nodeLabel = node.getLabel();
    PowersetIterator<Host> hostPowerset = new PowersetIterator<>(hostConfig.hostSet());
    for (Set<Host> hostSet : hostPowerset) {
      if (hostSet.size() > 1) {
        Label rLabel = Label.top();
        for (Host real : hostSet) {
          rLabel = rLabel.meet(hostConfig.getTrust(real));
        }

        if (rLabel.actsFor(nodeLabel)) {
          instances.add(new Replication(hostSet, new HashSet<>()));
        }
      }
    }

    return instances;
  }
}
