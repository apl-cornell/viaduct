package edu.cornell.cs.apl.viaduct.imp.backend.mamba;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.AbstractSynthesizedSingle;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.Set;

/** secret MAMBA protocol. */
public class MambaSecret extends AbstractSynthesizedSingle {
  private static Label computeLabel(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    Label label = Label.weakest();
    for (HostName party : hosts) {
      label = label.and(hostConfig.getTrust(party));
    }
    return label;
  }

  public MambaSecret(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    super(hosts, computeLabel(hostConfig, hosts));
  }

  @Override
  public String getId() {
    return "MambaSecret";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof MambaSecret) {
      MambaSecret omp = (MambaSecret)o;
      return this.hosts.equals(omp.hosts);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.hosts.hashCode();
  }
}
