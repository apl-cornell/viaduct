package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PowersetIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** read from smallest host set that has enough required integrity. */
public class ImpProtocolCommunicationStrategy implements ProtocolCommunicationStrategy<ImpAstNode> {
  @Override
  public Map<Host,Set<Host>> getCommunication(
      HostTrustConfiguration hostConfig,
      Protocol<ImpAstNode> fromProtocol, Protocol<ImpAstNode> toProtocol)
  {
    PowersetIterator<Host> fromHostPowerset = new PowersetIterator<>(fromProtocol.getHosts());
    Set<Host> toHostSet = toProtocol.getHosts();
    Map<Host,Set<Host>> communicationMap = new HashMap<>();

    // if the fromProtocol is MPC, just read directly from the synthesized host
    // otherwise, we will try to look for the synthesized host's trust in the
    // host configuration, but it will not exist!
    if (fromProtocol instanceof MPC) {
      for (Host toHost : toHostSet) {
        communicationMap.put(toHost, fromProtocol.getHosts());
      }

    } else {
      for (Set<Host> fromHostSet : fromHostPowerset) {
        Label fromLabel = Label.top();
        for (Host fromHost : fromHostSet) {
          fromLabel = fromLabel.meet(hostConfig.getTrust(fromHost));
        }

        // assign the first (smallest) subset of the fromHosts that have
        for (Host toHost : toHostSet) {
          if (!communicationMap.containsKey(toHost)) {
            Label toLabel = hostConfig.getTrust(toHost);
            if (fromLabel.integrity().actsFor(toLabel.integrity())) {
              communicationMap.put(toHost, fromHostSet);
            }
          }
        }

        if (communicationMap.size() == toHostSet.size()) {
          break;
        }
      }
    }

    assert (communicationMap.size() == toHostSet.size());
    return communicationMap;
  }
}
