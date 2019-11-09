package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

/** Protocol factory for protocols that can be summarized by a single label. */
public abstract class LabelProtocolFactory<T extends AstNode, I>
    implements ProtocolFactory<T>
{
  protected abstract Iterable<I> getHostInfo(HostTrustConfiguration hostConfig);

  protected abstract Protocol<T> createInstanceFromHostInfo(
      PdgNode<T> node, HostTrustConfiguration hostConfig, I hostInfo);

  protected Label getProtocolLabel(
      PdgNode<T> node,
      HostTrustConfiguration hostConfig,
      I hostInfo)
  {
    Protocol<T> protocolInstance = createInstanceFromHostInfo(node, hostConfig, hostInfo);
    return protocolInstance != null ? protocolInstance.getTrust() : null;
  }

  @Override
  public Set<Protocol<T>> createInstances(HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap, PdgNode<T> node)
  {
    Set<Protocol<T>> instances = new HashSet<>();
    Iterable<I> hostInfos = getHostInfo(hostConfig);

    for (I hostInfo : hostInfos) {
      Label protocolLabel = getProtocolLabel(node, hostConfig, hostInfo);
      if (protocolLabel != null && protocolLabel.actsFor(node.getLabel())) {
        instances.add(createInstanceFromHostInfo(node, hostConfig, hostInfo));
      }
    }

    return instances;
  }
}
