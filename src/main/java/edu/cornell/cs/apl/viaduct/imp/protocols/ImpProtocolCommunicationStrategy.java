package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.MemoizedProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PowersetIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** read from smallest host set that has enough required integrity. */
public final class ImpProtocolCommunicationStrategy
    extends MemoizedProtocolCommunicationStrategy<ImpAstNode>
{
  public ImpProtocolCommunicationStrategy() {
    super();
  }

  @Override
  protected Map<Host,Set<Host>> computeCommunicationMap(
      HostTrustConfiguration hostConfig,
      Protocol<ImpAstNode> fromProtocol, Protocol<ImpAstNode> toProtocol)
  {
    PowersetIterator<Host> fromHostPowerset = new PowersetIterator<>(fromProtocol.getHosts());
    Set<Host> toHostSet = toProtocol.getHosts();
    Map<Host,Set<Host>> communicationMap = new HashMap<>();

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

    assert (communicationMap.size() == toHostSet.size());
    return communicationMap;
  }

  @Override
  public Set<Host> getReadSet(
      HostTrustConfiguration hostConfig,
      Protocol<ImpAstNode> fromProtocol, Protocol<ImpAstNode> toProtocol,
      Host host)
  {
    // if the fromProtocol is MPC, just read directly from the synthesized host
    // otherwise, we will try to look for the synthesized host's trust in the
    // host configuration, but it will not exist!
    if (fromProtocol instanceof MPC || toProtocol instanceof MPC || host.isSynthetic()) {
      return fromProtocol.getHosts();

    } else if (toProtocol instanceof ControlProtocol) {
      PowersetIterator<Host> fromHostPowerset = new PowersetIterator<>(fromProtocol.getHosts());
      Label hostLabel = hostConfig.getTrust(host);

      for (Set<Host> fromHostSet : fromHostPowerset) {
        Label fromLabel = Label.top();
        for (Host fromHost : fromHostSet) {
          fromLabel = fromLabel.meet(hostConfig.getTrust(fromHost));
        }

        if (fromLabel.integrity().actsFor(hostLabel.integrity())) {
          return fromHostSet;
        }
      }
      throw new ProtocolInstantiationError("control node cannot read with enough integrity");

    } else {
      Map<Host,Set<Host>> communicationMap =
          getCommunicationMap(hostConfig, fromProtocol, toProtocol);
      return communicationMap.get(host);
    }
  }

  @Override
  public Set<Host> getWriteSet(
      HostTrustConfiguration hostConfig,
      Protocol<ImpAstNode> fromProtocol, Protocol<ImpAstNode> toProtocol,
      Host host)
  {
    if (toProtocol instanceof ControlProtocol) {
      throw new ProtocolInstantiationError("control protocol is selected for storage node");

    } else if (fromProtocol instanceof MPC) {
      return toProtocol.getHosts();

    } else {
      Map<Host,Set<Host>> communicationMap =
          getCommunicationMap(hostConfig, fromProtocol, toProtocol);
      Set<Host> writeSet = new HashSet<>();
      for (Map.Entry<Host,Set<Host>> kv : communicationMap.entrySet()) {
        if (kv.getValue().contains(host)) {
          writeSet.add(kv.getKey());
        }
      }

      return writeSet;
    }
  }
}
