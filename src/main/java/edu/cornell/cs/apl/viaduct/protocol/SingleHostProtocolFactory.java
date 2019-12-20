package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.collection.Map;

public abstract class SingleHostProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, HostName> {
  @Override
  protected Iterable<HostName> getHostInfo(HostTrustConfiguration hostConfig) {
    return hostConfig.hosts();
  }

  @Override
  protected Label getProtocolLabel(
      PdgNode<T> node,
      Map<PdgNode<T>, Protocol<T>> protocolMap,
      HostTrustConfiguration hostConfig,
      HostName host) {
    return hostConfig.getTrust(host);
  }
}
