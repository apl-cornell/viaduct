package edu.cornell.cs.apl.viaduct.backend.mamba;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.AllHostsProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import java.util.Set;

/** cleartext data in a MAMBA program. */
public class MambaPublicFactory extends AllHostsProtocolFactory<ImpAstNode> {
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      HostTrustConfiguration hostConfig, Set<HostName> hostSet)
  {
    return hostSet.size() >= 2 ? new MambaPublic(hostConfig, hostSet) : null;
  }
}
