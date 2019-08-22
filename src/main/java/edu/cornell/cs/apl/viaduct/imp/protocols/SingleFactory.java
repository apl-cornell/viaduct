package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.SingleHostProtocolFactory;

/** contains Single protocol information flow constraints. */
public class SingleFactory extends SingleHostProtocolFactory<ImpAstNode> {
  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      HostTrustConfiguration hostConfig, Host host) {
    return new Single(hostConfig, host);
  }
}
