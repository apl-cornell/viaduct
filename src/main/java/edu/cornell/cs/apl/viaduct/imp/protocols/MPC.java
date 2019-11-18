package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.Set;

/** multiparty computation protocol. */
public class MPC extends AbstractSynthesizedSingle {
  private static Label computeLabel(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    Label label = Label.weakest();
    for (HostName party : hosts) {
      label = label.and(hostConfig.getTrust(party));
    }
    return label;
  }

  /** constructor. */
  public MPC(HostTrustConfiguration hostConfig, Set<HostName> ps) {
    super(ps, computeLabel(hostConfig, ps));
  }

  @Override
  protected Object getProcessIdentity() {
    return this.hosts;
  }

  @Override
  public String getId() {
    return "MPC";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof MPC) {
      MPC ompc = (MPC) o;
      return this.hosts.equals(ompc.hosts);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.hosts.hashCode();
  }
}
