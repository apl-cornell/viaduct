package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;
import io.vavr.collection.HashSet;
import java.util.Set;

/** return a singleton host info set: the set of all hosts in the host config. */
public abstract class AllHostsProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, Set<HostName>> {

  @Override
  protected Iterable<Set<HostName>> getHostInfo(HostTrustConfiguration hostConfig) {
    return HashSet.of(hostConfig.hosts());
  }
}
