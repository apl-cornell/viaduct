package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashSet;
import java.util.Set;

public class Single extends AbstractSingle {
  private final HostName host;
  private final Label trust;

  /** constructor. */
  public Single(HostTrustConfiguration hostConfig, HostName h) {
    this.host = h;
    this.trust = hostConfig.getTrust(h);
  }

  public HostName getHost() {
    return this.host;
  }

  @Override
  public Label getTrust() {
    return this.trust;
  }

  @Override
  protected HostName getActualHost() {
    return this.host;
  }

  @Override
  public String getId() {
    return "Single";
  }

  @Override
  public Set<HostName> getHosts() {
    Set<HostName> hosts = new HashSet<>();
    hosts.add(this.host);
    return hosts;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Single) {
      Single osingle = (Single) o;
      return this.host.equals(osingle.host);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.host.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Single(%s)", this.host.toString());
  }
}
