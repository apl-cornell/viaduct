package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import java.util.HashSet;
import java.util.Set;

/** abstract protocol over a set of hosts. */
public abstract class AbstractProtocol<T extends AstNode> implements Protocol<T> {
  protected final Set<HostName> hosts;

  protected AbstractProtocol(Set<HostName> hosts) {
    this.hosts = hosts;
  }

  protected AbstractProtocol(HostName host) {
    this.hosts = new HashSet<>();
    this.hosts.add(host);
  }

  @Override
  public Set<HostName> getHosts() {
    return this.hosts;
  }

  @Override
  public String toString() {
    HashSet<String> strs = new HashSet<>();
    for (HostName party : this.hosts) {
      strs.add(party.toString());
    }

    String strList = String.join(",", strs);
    return String.format("%s(%s)", getId(), strList);
  }
}
