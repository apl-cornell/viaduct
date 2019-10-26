package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.InvalidProtocolException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;

import java.util.Set;

/** determines how protocols will communicate with each other. */
public interface ProtocolCommunicationStrategy<T extends AstNode> {
  /** get the set of processes to read from. */
  Set<ProcessName> getReadSet(
      HostTrustConfiguration hostConfig,
      Protocol<T> fromProtocol,
      Protocol<T> toProtocol,
      ProcessName process) throws InvalidProtocolException;

  /* get the set of hosts to write to. */
  Set<ProcessName> getWriteSet(
      HostTrustConfiguration hostConfig,
      Protocol<T> fromProtocol,
      Protocol<T> toProtocol,
      ProcessName process) throws InvalidProtocolException;
}
