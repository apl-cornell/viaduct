package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** multiparty computation protocol. */
public class MPC extends AbstractSingle {
  private static final Map<Set<HostName>, HostName> synthesizedHostMap = new HashMap<>();
  private final Set<HostName> parties;
  private final Label trust;
  private HostName synthesizedHost;

  /** constructor. */
  public MPC(HostTrustConfiguration hostConfig, Set<HostName> ps) {
    this.parties = ps;
    this.synthesizedHost = HostName.create(toString());

    Label label = Label.weakest();
    for (HostName party : this.parties) {
      label = label.and(hostConfig.getTrust(party));
    }
    this.trust = label;
  }

  public Set<HostName> getParties() {
    return this.parties;
  }

  public HostName getHost() {
    return this.synthesizedHost;
  }

  @Override
  public Label getTrust() {
    return trust;
  }

  @Override
  public String getId() {
    return "MPC";
  }

  @Override
  protected HostName getActualHost() {
    return this.synthesizedHost;
  }

  @Override
  public Set<HostName> getHosts() {
    Set<HostName> hosts = new HashSet<>();
    hosts.add(this.synthesizedHost);
    return hosts;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (synthesizedHostMap.containsKey(this.parties)) {
      this.synthesizedHost = synthesizedHostMap.get(this.parties);

    } else {
      this.synthesizedHost = HostName.create(info.getFreshName(toString()), true);
      synthesizedHostMap.put(this.parties, this.synthesizedHost);
      info.createProcess(this.synthesizedHost);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof MPC) {
      MPC ompc = (MPC) o;
      return this.parties.equals(ompc.parties);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.parties.hashCode();
  }

  @Override
  public String toString() {
    HashSet<String> strs = new HashSet<>();
    for (HostName party : this.parties) {
      strs.add(party.toString());
    }

    String strList = String.join(",", strs);
    return String.format("MPC(%s)", strList);
  }
}
