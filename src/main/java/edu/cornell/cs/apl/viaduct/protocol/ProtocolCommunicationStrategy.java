package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import java.util.Set;

/** determines how protocols will communicate with each other. */
public interface ProtocolCommunicationStrategy<T extends AstNode> {
  /** get the set of hosts to read from. */
  Set<HostName> getReadSet(
      HostTrustConfiguration hostConfig,
      Protocol<T> fromProtocol,
      Protocol<T> toProtocol,
      HostName host);

  /* get the set of hosts to write to. */
  Set<HostName> getWriteSet(
      HostTrustConfiguration hostConfig,
      Protocol<T> fromProtocol,
      Protocol<T> toProtocol,
      HostName host);
}
