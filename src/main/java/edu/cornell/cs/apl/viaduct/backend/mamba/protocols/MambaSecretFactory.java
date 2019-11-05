package edu.cornell.cs.apl.viaduct.backend.mamba.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.AllHostsProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import java.util.Set;

/** cleartext data in a MAMBA program. */
public class MambaSecretFactory extends AllHostsProtocolFactory<ImpAstNode> {
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      HostTrustConfiguration hostConfig,
      Set<HostName> hostSet)
  {
    if (!node.isArrayIndex() && !node.isLoopGuard()) {
      return hostSet.size() >= 2 ? new MambaSecret(hostConfig, hostSet) : null;

    } else {
      return null;
    }
  }
}
