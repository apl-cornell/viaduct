package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PowersetIterator;

import java.util.HashSet;
import java.util.Set;

public abstract class PowersetProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, Set<Host>>
{
  protected abstract Label getProtocolLabelFromLabelSet(Set<Label> hostLabels);

  @Override
  protected Iterable<Set<Host>> getHostInfo(HostTrustConfiguration hostConfig) {
    return new PowersetIterator<>(hostConfig.hostSet());
  }

  @Override
  protected Label getProtocolLabel(HostTrustConfiguration hostConfig, Set<Host> hostSet) {
    Set<Label> hostLabels = new HashSet<>();
    for (Host host : hostSet) {
      hostLabels.add(hostConfig.getTrust(host));
    }
    return getProtocolLabelFromLabelSet(hostLabels);
  }
}
