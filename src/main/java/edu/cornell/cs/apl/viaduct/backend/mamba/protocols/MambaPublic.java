package edu.cornell.cs.apl.viaduct.backend.mamba.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.AbstractSynthesizedSingle;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Set;

/** cleartext MAMBA protocol. */
public class MambaPublic extends AbstractSynthesizedSingle {
  public static final String PROTOCOL_ID = "MambaPublic";

  private static Label computeLabel(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    Label label = Label.top();
    for (HostName party : hosts) {
      label = label.meet(hostConfig.getTrust(party));
    }
    return label;
  }

  public MambaPublic(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    super(hosts, computeLabel(hostConfig, hosts));
  }

  @Override
  protected Object getProcessIdentity() {
    return this.hosts;
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

    if (o instanceof MambaPublic) {
      MambaPublic omp = (MambaPublic) o;
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
