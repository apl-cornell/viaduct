package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.PowersetProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import java.util.Set;

/** contains MPC information flow constraints. */
public class MPCFactory extends PowersetProtocolFactory<ImpAstNode> {
  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      HostTrustConfiguration hostConfig, Set<Host> hostSet) {
    return new MPC(hostConfig, hostSet);
  }
}
