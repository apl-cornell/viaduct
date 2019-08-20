package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;

public abstract class SingleHostProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, Host>
{
  @Override
  protected Iterable<Host> getHostInfo(HostTrustConfiguration hostConfig) {
    return hostConfig.hostSet();
  }

  @Override
  protected Label getProtocolLabel(HostTrustConfiguration hostConfig, Host host) {
    return hostConfig.getTrust(host);
  }
}
