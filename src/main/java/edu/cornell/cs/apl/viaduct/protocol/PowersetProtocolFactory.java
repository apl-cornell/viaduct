package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;
import edu.cornell.cs.apl.viaduct.util.PowersetIterator;
import java.util.Set;

public abstract class PowersetProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, Set<HostName>> {
  @Override
  protected Iterable<Set<HostName>> getHostInfo(HostTrustConfiguration hostConfig) {
    return new PowersetIterator<>(hostConfig.hosts());
  }
}
