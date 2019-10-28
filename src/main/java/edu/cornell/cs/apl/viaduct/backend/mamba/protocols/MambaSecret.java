package edu.cornell.cs.apl.viaduct.backend.mamba.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.AbstractSynthesizedSingle;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.Set;

/** secret MAMBA protocol. */
public class MambaSecret extends AbstractSynthesizedSingle {
  public static final String PROTOCOL_ID = "MambaSecret";

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
    return PROTOCOL_ID;
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
