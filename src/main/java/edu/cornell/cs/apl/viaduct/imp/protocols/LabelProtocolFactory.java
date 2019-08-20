package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Protocol factory for protocols that can be summarized by a single label. */
public abstract class LabelProtocolFactory<T extends AstNode, I>
    implements ProtocolFactory<T>
{
  protected abstract Iterable<I> getHostInfo(HostTrustConfiguration hostConfig);

  protected abstract Protocol<T> createInstanceFromHostInfo(I hostInfo);

  protected abstract Label getProtocolLabel(HostTrustConfiguration hostConfig, I hostInfo);

  @Override
  public Set<Protocol<T>> createInstances(HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap, PdgNode<T> node)
  {
    Set<Protocol<T>> instances = new HashSet<>();
    Iterable<I> hostInfos = getHostInfo(hostConfig);

    for (I hostInfo : hostInfos) {
      Label protocolLabel = getProtocolLabel(hostConfig, hostInfo);
      if (protocolLabel != null && protocolLabel.actsFor(node.getLabel())) {
        instances.add(createInstanceFromHostInfo(hostInfo));
      }
    }

    return instances;
  }
}
