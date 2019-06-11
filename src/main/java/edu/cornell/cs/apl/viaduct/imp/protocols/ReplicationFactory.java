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
    if (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode()) {
      Label nInLabel = node.getInLabel();

      PowersetIterator<Host> hostPowerset = new PowersetIterator<>(hostConfig.hostSet());
      for (Set<Host> hostSet : hostPowerset) {
        Label rLabel = Label.top();

        /*
        Label rhLabel = Label.top();
        for (Host real : possibleInstance.realReplicas) {
          rLabel = rLabel.meet(hostConfig.getTrust(real));
          rhLabel = rhLabel.meet(hostConfig.getTrust(real));
        }
        for (Host hash : possibleInstance.hashReplicas) {
          rhLabel = rhLabel.meet(hostConfig.getTrust(hash));
        }
        */

        for (Host real : hostSet) {
          rLabel = rLabel.meet(hostConfig.getTrust(real));
        }

        if (nInLabel.confidentiality().flowsTo(rLabel.confidentiality())
            && rLabel.integrity().flowsTo(nInLabel.integrity())
            && hostSet.size() > 1) {

          // control nodes can't be hash replicated!
          // && !(node.isControlNode() && possibleInstance.hashReplicas.size() > 0)
          // has to be more than 1 replica to actually be replicated
          // && possibleInstance.realReplicas.size() + possibleInstance.hashReplicas.size() > 1) {

          instances.add(new Replication(hostSet, new HashSet<>()));
        }
      }
    }

    return instances;
  }
}
